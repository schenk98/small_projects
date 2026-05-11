from __future__ import annotations

import re

from bs4 import BeautifulSoup


def extract_links_and_assets(soup: BeautifulSoup, base_url: str) -> tuple[list[str], list[str]]:
    links: list[str] = []
    assets: list[str] = []

    for a in soup.select("a[href]"):
        href = a.get("href")
        if href:
            links.append(href)

    for tag_name, attr in [
        ("img", "src"),
        ("source", "src"),
        ("video", "src"),
        ("audio", "src"),
        ("link", "href"),
        ("script", "src"),
    ]:
        for t in soup.select(f"{tag_name}[{attr}]"):
            v = t.get(attr)
            if v:
                assets.append(v)

    # Some PDFs/docs appear as links rather than <link>
    for a in soup.select("a[href]"):
        href = a.get("href") or ""
        if re.search(r"\.(pdf|docx?|xlsx?|pptx?|odt|ods|odp)$", href, flags=re.IGNORECASE):
            assets.append(href)

    # Dedup, preserve order
    def dedup(items: list[str]) -> list[str]:
        seen: set[str] = set()
        out: list[str] = []
        for x in items:
            if x not in seen:
                seen.add(x)
                out.append(x)
        return out

    return dedup(links), dedup(assets)


def extract_text_best_effort(html: str) -> tuple[str, list[str]]:
    """
    Return (text, headings).
    Pure-Python extraction (Python 3.14 friendly):
    - parse HTML and return visible text + headings
    """
    headings: list[str] = []

    soup = BeautifulSoup(html, "html.parser")
    for h in soup.select("h1,h2,h3"):
        t = h.get_text(" ", strip=True)
        if t:
            headings.append(t)
    return soup.get_text("\n", strip=True).strip(), headings

