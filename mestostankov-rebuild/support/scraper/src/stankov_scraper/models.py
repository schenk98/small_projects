from __future__ import annotations

from dataclasses import asdict, dataclass, field
from enum import Enum
from typing import Any, Literal


class FetchKind(str, Enum):
    http = "http"
    browser = "browser"


class ContentType(str, Enum):
    homepage = "homepage"
    static_page = "static_page"
    contact = "contact"

    news_list = "news_list"
    news_item = "news_item"

    events_list = "events_list"
    events_item = "events_item"

    official_board_list = "official_board_list"
    official_board_item = "official_board_item"

    gallery_list = "gallery_list"
    gallery_item = "gallery_item"

    document = "document"
    unknown = "unknown"


@dataclass(slots=True)
class CrawlConfig:
    seed: str
    out: str
    allowed_domains: list[str] = field(default_factory=lambda: ["www.mestostankov.cz", "mestostankov.cz"])
    max_pages: int = 2500
    concurrency: int = 8
    delay_ms: int = 200
    user_agent: str = "mestostankov-rebuild-support-scraper/0.1 (+https://www.mestostankov.cz/)"
    download_assets: bool = False
    use_browser_fallback: bool = False


@dataclass(slots=True)
class NormalizedPage:
    id: str
    url: str
    canonical_url: str | None = None
    lang: str | None = None
    title: str | None = None
    fetched_at_iso: str = ""
    fetch_kind: FetchKind = FetchKind.http
    http_status: int | None = None

    content_type: ContentType = ContentType.unknown
    content_subtype: str | None = None

    headings: list[str] = field(default_factory=list)
    text: str = ""
    excerpt: str | None = None

    discovered_links: list[str] = field(default_factory=list)
    discovered_assets: list[str] = field(default_factory=list)

    raw_html_path: str = ""
    raw_text_path: str = ""
    raw_meta_path: str = ""

    extra: dict[str, Any] = field(default_factory=dict)

    def to_dict(self) -> dict[str, Any]:
        d = asdict(self)
        d["fetch_kind"] = self.fetch_kind.value
        d["content_type"] = self.content_type.value
        return d


@dataclass(slots=True)
class AssetRecord:
    id: str
    url: str
    source_page_id: str
    content_type: str | None = None
    size_bytes: int | None = None
    sha256: str | None = None
    path: str | None = None
    kind: Literal["downloaded", "referenced_only"] = "referenced_only"

    def to_dict(self) -> dict[str, Any]:
        return asdict(self)

