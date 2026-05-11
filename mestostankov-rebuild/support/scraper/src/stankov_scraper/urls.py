from __future__ import annotations

import re
from urllib.parse import urljoin, urlparse, urlunparse, parse_qsl, urlencode


def normalize_url(base_url: str, href: str) -> str | None:
    if not href:
        return None

    href = href.strip()
    if href.startswith(("mailto:", "tel:", "javascript:", "#")):
        return None

    abs_url = urljoin(base_url, href)
    parsed = urlparse(abs_url)

    if parsed.scheme not in ("http", "https"):
        return None

    # Drop fragments
    parsed = parsed._replace(fragment="")

    # Normalize query params ordering and strip tracking params
    query_pairs = [(k, v) for (k, v) in parse_qsl(parsed.query, keep_blank_values=True)]
    query_pairs = [(k, v) for (k, v) in query_pairs if k.lower() not in {"utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content"}]
    query_pairs.sort(key=lambda kv: kv[0])
    parsed = parsed._replace(query=urlencode(query_pairs, doseq=True))

    # Normalize default ports
    netloc = parsed.netloc
    if netloc.endswith(":80") and parsed.scheme == "http":
        netloc = netloc[:-3]
    if netloc.endswith(":443") and parsed.scheme == "https":
        netloc = netloc[:-4]
    parsed = parsed._replace(netloc=netloc)

    return urlunparse(parsed)


def is_same_domain(url: str, allowed_domains: list[str]) -> bool:
    host = urlparse(url).hostname or ""
    host = host.lower()
    return any(host == d.lower() or host.endswith("." + d.lower()) for d in allowed_domains)


_doc_ext_re = re.compile(r"\.(pdf|doc|docx|xls|xlsx|ppt|pptx|odt|ods|odp|rtf|txt)$", re.IGNORECASE)
_img_ext_re = re.compile(r"\.(png|jpg|jpeg|gif|webp|svg)$", re.IGNORECASE)


def looks_like_document(url: str) -> bool:
    return bool(_doc_ext_re.search(urlparse(url).path))


def looks_like_image(url: str) -> bool:
    return bool(_img_ext_re.search(urlparse(url).path))

