from __future__ import annotations

import asyncio
from dataclasses import dataclass
from typing import Optional

import httpx
from tenacity import retry, stop_after_attempt, wait_exponential_jitter

from .models import FetchKind


@dataclass
class FetchResult:
    url: str
    status_code: int | None
    html: str | None
    fetch_kind: FetchKind
    final_url: str | None = None
    error: str | None = None


class HttpFetcher:
    def __init__(self, user_agent: str, timeout_s: float = 30.0):
        self._client = httpx.AsyncClient(
            follow_redirects=True,
            headers={"User-Agent": user_agent, "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"},
            timeout=httpx.Timeout(timeout_s),
        )

    async def aclose(self) -> None:
        await self._client.aclose()

    @retry(stop=stop_after_attempt(3), wait=wait_exponential_jitter(initial=0.5, max=8.0))
    async def fetch_html(self, url: str) -> FetchResult:
        try:
            resp = await self._client.get(url)
            content_type = (resp.headers.get("content-type") or "").lower()
            if "text/html" not in content_type and "application/xhtml" not in content_type:
                return FetchResult(url=url, status_code=resp.status_code, html=None, fetch_kind=FetchKind.http, final_url=str(resp.url))
            return FetchResult(url=url, status_code=resp.status_code, html=resp.text, fetch_kind=FetchKind.http, final_url=str(resp.url))
        except Exception as e:
            return FetchResult(url=url, status_code=None, html=None, fetch_kind=FetchKind.http, error=str(e))


class BrowserFetcher:
    """
    Fallback for pages that require JS rendering.
    Kept intentionally conservative: only used when HTTP fetch returns no HTML.
    """

    def __init__(self, user_agent: str, navigation_timeout_ms: int = 30000):
        self._user_agent = user_agent
        self._navigation_timeout_ms = navigation_timeout_ms
        self._started = False
        self._playwright = None
        self._browser = None

    async def _ensure_started(self) -> None:
        if self._started:
            return
        from playwright.async_api import async_playwright  # local import to keep import-time light

        self._playwright = await async_playwright().start()
        self._browser = await self._playwright.chromium.launch(headless=True)
        self._started = True

    async def aclose(self) -> None:
        if self._browser:
            await self._browser.close()
        if self._playwright:
            await self._playwright.stop()

    async def fetch_html(self, url: str) -> FetchResult:
        try:
            await self._ensure_started()
            assert self._browser is not None
            context = await self._browser.new_context(user_agent=self._user_agent)
            page = await context.new_page()
            page.set_default_navigation_timeout(self._navigation_timeout_ms)
            resp = await page.goto(url, wait_until="networkidle")
            html = await page.content()
            final_url = page.url
            status = resp.status if resp else None
            await context.close()
            return FetchResult(url=url, status_code=status, html=html, fetch_kind=FetchKind.browser, final_url=final_url)
        except Exception as e:
            return FetchResult(url=url, status_code=None, html=None, fetch_kind=FetchKind.browser, error=str(e))


async def polite_delay(delay_ms: int) -> None:
    if delay_ms <= 0:
        return
    await asyncio.sleep(delay_ms / 1000.0)

