from __future__ import annotations

import asyncio
from collections import deque
from dataclasses import dataclass
from datetime import datetime, timezone
from typing import Iterable
from urllib.parse import urlparse

import httpx
from bs4 import BeautifulSoup

from .classify import classify_page
from .extract import extract_links_and_assets, extract_text_best_effort
from .fetch import BrowserFetcher, HttpFetcher, polite_delay
from .storage import RunStore, stable_id, utc_now_iso
from .models import AssetRecord, ContentType, CrawlConfig, NormalizedPage
from .urls import is_same_domain, looks_like_document, looks_like_image, normalize_url


@dataclass
class QueueItem:
    url: str
    discovered_from: str | None = None


def _dedup_preserve(items: Iterable[str]) -> list[str]:
    seen: set[str] = set()
    out: list[str] = []
    for x in items:
        if x not in seen:
            seen.add(x)
            out.append(x)
    return out


async def _download_asset(url: str, dest_path: str, user_agent: str) -> tuple[int | None, int | None, str | None, str | None]:
    try:
        async with httpx.AsyncClient(
            follow_redirects=True,
            headers={"User-Agent": user_agent, "Accept": "*/*"},
            timeout=httpx.Timeout(60.0),
        ) as client:
            resp = await client.get(url)
            ct = resp.headers.get("content-type")
            data = resp.content
            size = len(data)
            import hashlib

            sha = hashlib.sha256(data).hexdigest()
            PathLike = dest_path  # keep mypy calm without importing Path
            with open(PathLike, "wb") as f:
                f.write(data)
            return resp.status_code, size, sha, ct
    except Exception as e:
        return None, None, None, f"{e}"


async def crawl(config: CrawlConfig) -> None:
    store = RunStore(config.out)
    http_fetcher = HttpFetcher(user_agent=config.user_agent)
    browser_fetcher = BrowserFetcher(user_agent=config.user_agent) if config.use_browser_fallback else None

    store.write_run_metadata(
        {
            "seed": config.seed,
            "started_at": utc_now_iso(),
            "allowed_domains": config.allowed_domains,
            "max_pages": config.max_pages,
            "concurrency": config.concurrency,
            "delay_ms": config.delay_ms,
            "download_assets": config.download_assets,
            "use_browser_fallback": config.use_browser_fallback,
        }
    )

    q: deque[QueueItem] = deque([QueueItem(url=config.seed, discovered_from=None)])
    seen: set[str] = set()
    in_flight: set[str] = set()
    processed = 0
    last_printed = 0
    print_lock = asyncio.Lock()

    async def worker() -> None:
        nonlocal processed, last_printed

        while True:
            # Stop conditions:
            # - hard cap reached
            # - crawl frontier exhausted (no queued URLs and nothing fetching)
            if processed >= config.max_pages:
                return
            if not q and not in_flight:
                return

            try:
                item = q.popleft()
            except IndexError:
                await asyncio.sleep(0.05)
                continue

            if item.url in seen or item.url in in_flight:
                continue
            if not is_same_domain(item.url, config.allowed_domains):
                continue

            # Skip obvious non-html docs in the page queue; we capture them as assets instead.
            if looks_like_document(item.url) or looks_like_image(item.url):
                continue

            in_flight.add(item.url)

            await polite_delay(config.delay_ms)

            page_id = stable_id(item.url)
            cache_key = f"page:{item.url}"
            cached = store.cache_get(cache_key)
            if cached:
                in_flight.discard(item.url)
                seen.add(item.url)
                continue

            fetch_res = await http_fetcher.fetch_html(item.url)
            if (not fetch_res.html) and browser_fetcher is not None:
                fetch_res = await browser_fetcher.fetch_html(item.url)

            html = fetch_res.html or ""
            soup = BeautifulSoup(html, "html.parser") if html else None

            content_type, subtype = classify_page(fetch_res.final_url or item.url, soup)

            if soup is not None:
                raw_links, raw_assets = extract_links_and_assets(soup, fetch_res.final_url or item.url)
            else:
                raw_links, raw_assets = [], []

            norm_links: list[str] = []
            for href in raw_links:
                u = normalize_url(fetch_res.final_url or item.url, href)
                if u and is_same_domain(u, config.allowed_domains):
                    norm_links.append(u)

            norm_assets: list[str] = []
            for href in raw_assets:
                u = normalize_url(fetch_res.final_url or item.url, href)
                if u:
                    norm_assets.append(u)

            norm_links = _dedup_preserve(norm_links)
            norm_assets = _dedup_preserve(norm_assets)

            text, headings = extract_text_best_effort(html) if html else ("", [])
            excerpt = (text[:240] + "…") if len(text) > 240 else (text or None)

            meta = {
                "url": item.url,
                "final_url": fetch_res.final_url,
                "status_code": fetch_res.status_code,
                "fetch_kind": fetch_res.fetch_kind,
                "error": fetch_res.error,
                "discovered_from": item.discovered_from,
                "content_type": content_type,
                "content_subtype": subtype,
            }

            raw_html_path, raw_text_path, raw_meta_path = store.write_raw(page_id=page_id, html=html, text=text, meta=meta)

            page = NormalizedPage(
                id=page_id,
                url=item.url,
                canonical_url=fetch_res.final_url,
                lang=(soup.html.get("lang") if soup and soup.html else None),
                title=(soup.title.get_text(" ", strip=True) if soup and soup.title else None),
                fetched_at_iso=utc_now_iso(),
                fetch_kind=fetch_res.fetch_kind,
                http_status=fetch_res.status_code,
                content_type=content_type,
                content_subtype=subtype,
                headings=headings,
                text=text,
                excerpt=excerpt,
                discovered_links=norm_links,
                discovered_assets=norm_assets,
                raw_html_path=raw_html_path,
                raw_text_path=raw_text_path,
                raw_meta_path=raw_meta_path,
                extra={},
            )

            store.append_page(page.to_dict())
            store.append_url_map({"url": item.url, "page_id": page_id, "final_url": fetch_res.final_url})
            store.cache_set(cache_key, {"page_id": page_id, "ts": utc_now_iso()})

            # Enqueue discovered internal pages
            for u in norm_links:
                if u not in seen and u not in in_flight and len(seen) + len(in_flight) + len(q) < config.max_pages * 4:
                    q.append(QueueItem(url=u, discovered_from=page_id))

            # Asset handling
            if norm_assets:
                for asset_url in norm_assets:
                    asset_id = stable_id(f"{page_id}:{asset_url}")
                    rec = AssetRecord(id=asset_id, url=asset_url, source_page_id=page_id)

                    if config.download_assets and is_same_domain(asset_url, config.allowed_domains) and looks_like_document(asset_url):
                        # Save documents into assets/files, keep extension if possible
                        from pathlib import Path
                        import os

                        ext = os.path.splitext(urlparse(asset_url).path)[1].lower()
                        if not ext:
                            ext = ".bin"
                        dest = store.assets_dir / f"{asset_id}{ext}"

                        status, size, sha, ct = await _download_asset(asset_url, str(dest), config.user_agent)
                        rec.kind = "downloaded" if status and status < 400 else "referenced_only"
                        rec.size_bytes = size
                        rec.sha256 = sha
                        rec.content_type = ct
                        rec.path = str(dest) if rec.kind == "downloaded" else None

                    store.append_asset(rec.to_dict())

            processed += 1
            seen.add(item.url)
            in_flight.discard(item.url)
            if processed - last_printed >= 25:
                async with print_lock:
                    if processed - last_printed >= 25:
                        last_printed = processed
                        print(f"[crawl] pages={processed} queue={len(q)} seen={len(seen)} in_flight={len(in_flight)}")

    try:
        workers = [asyncio.create_task(worker()) for _ in range(config.concurrency)]
        await asyncio.gather(*workers)
    finally:
        await http_fetcher.aclose()
        if browser_fetcher is not None:
            await browser_fetcher.aclose()
        store.close()

