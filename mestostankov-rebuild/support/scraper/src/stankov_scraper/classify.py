from __future__ import annotations

import re
from urllib.parse import urlparse

from bs4 import BeautifulSoup

from .models import ContentType


_slug_rules: list[tuple[re.Pattern[str], ContentType]] = [
    (re.compile(r"^/$"), ContentType.homepage),
    (re.compile(r"/kontakty/?"), ContentType.contact),
    (re.compile(r"/informace-pro-obcany/aktuality/?"), ContentType.news_list),
    (re.compile(r"/informace-pro-obcany/aktuality/.+"), ContentType.news_item),
    (re.compile(r"/informace-pro-obcany/kalendar-akci/?"), ContentType.events_list),
    (re.compile(r"/informace-pro-obcany/kalendar-akci/.+"), ContentType.events_item),
    (re.compile(r"/urad/uredni-deska/?"), ContentType.official_board_list),
    (re.compile(r"/urad/uredni-deska/.+"), ContentType.official_board_item),
    (re.compile(r"/fotogalerie/?"), ContentType.gallery_list),
    (re.compile(r"/fotogalerie/.+"), ContentType.gallery_item),
]


def classify_url(url: str) -> ContentType:
    path = urlparse(url).path or "/"
    for pattern, ct in _slug_rules:
        if pattern.search(path):
            return ct
    return ContentType.unknown


def classify_page(url: str, soup: BeautifulSoup | None) -> tuple[ContentType, str | None]:
    """
    Classify primarily by content intent, using URL as a hint.
    Returns (content_type, subtype).
    """
    ct = classify_url(url)
    subtype: str | None = None

    if soup is None:
        return ct if ct != ContentType.unknown else ContentType.static_page, None

    title = (soup.title.get_text(" ", strip=True) if soup.title else "").lower()
    h1 = (soup.find("h1").get_text(" ", strip=True) if soup.find("h1") else "").lower()
    page_label = f"{title} {h1}".strip()

    # Content-first heuristics (Czech labels)
    if any(k in page_label for k in ["úřední deska", "uredni deska"]):
        return (ContentType.official_board_list if ct in (ContentType.unknown, ContentType.static_page) else ct), None
    if any(k in page_label for k in ["aktuality", "zprávy", "zpravy"]):
        return (ContentType.news_list if ct in (ContentType.unknown, ContentType.static_page) else ct), None
    if any(k in page_label for k in ["kalendář akcí", "kalendar akci", "akce"]):
        return (ContentType.events_list if ct in (ContentType.unknown, ContentType.static_page) else ct), None
    if any(k in page_label for k in ["fotogalerie", "foto galerie", "galerie"]):
        return (ContentType.gallery_list if ct in (ContentType.unknown, ContentType.static_page) else ct), None
    if any(k in page_label for k in ["kontakt", "kontakty"]):
        return (ContentType.contact if ct in (ContentType.unknown, ContentType.static_page) else ct), None

    # Galileo pages frequently end with ..._####cs.html. Treat as static unless we detect lists above.
    if re.search(r"_\d+(cs|en|de)\.html$", url, flags=re.IGNORECASE):
        subtype = "legacy_id_page"
        return (ct if ct != ContentType.unknown else ContentType.static_page), subtype

    return (ct if ct != ContentType.unknown else ContentType.static_page), subtype

