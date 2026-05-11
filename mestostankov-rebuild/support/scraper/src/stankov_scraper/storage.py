from __future__ import annotations

import hashlib
import json
import os
from datetime import datetime, timezone
from pathlib import Path
from typing import Any

from diskcache import Cache


def utc_now_iso() -> str:
    return datetime.now(timezone.utc).isoformat()


def sha256_hex(data: bytes) -> str:
    return hashlib.sha256(data).hexdigest()


def stable_id(url: str) -> str:
    return sha256_hex(url.encode("utf-8"))[:24]


class RunStore:
    def __init__(self, out_dir: str):
        self.out_dir = Path(out_dir)
        self.raw_pages_dir = self.out_dir / "raw" / "pages"
        self.raw_text_dir = self.out_dir / "raw" / "text"
        self.raw_meta_dir = self.out_dir / "raw" / "meta"
        self.assets_dir = self.out_dir / "assets" / "files"
        self.index_dir = self.out_dir / "index"
        self.norm_dir = self.out_dir / "normalized"
        self.logs_dir = self.out_dir / "logs"

        for p in [
            self.raw_pages_dir,
            self.raw_text_dir,
            self.raw_meta_dir,
            self.assets_dir,
            self.index_dir,
            self.norm_dir,
            self.logs_dir,
        ]:
            p.mkdir(parents=True, exist_ok=True)

        self._cache = Cache(directory=str(self.out_dir / ".cache"))

        self.pages_jsonl = open(self.norm_dir / "pages.jsonl", "ab")
        self.assets_jsonl = open(self.norm_dir / "assets.jsonl", "ab")
        self.url_map_jsonl = open(self.index_dir / "url_map.jsonl", "ab")

    def close(self) -> None:
        self.pages_jsonl.close()
        self.assets_jsonl.close()
        self.url_map_jsonl.close()
        self._cache.close()

    def cache_get(self, key: str) -> Any | None:
        return self._cache.get(key, default=None)

    def cache_set(self, key: str, value: Any, expire_s: int | None = None) -> None:
        self._cache.set(key, value, expire=expire_s)

    def write_raw(self, page_id: str, html: str, text: str, meta: dict[str, Any]) -> tuple[str, str, str]:
        html_path = self.raw_pages_dir / f"{page_id}.html"
        text_path = self.raw_text_dir / f"{page_id}.txt"
        meta_path = self.raw_meta_dir / f"{page_id}.json"

        html_path.write_text(html, encoding="utf-8", errors="ignore")
        text_path.write_text(text, encoding="utf-8", errors="ignore")
        meta_path.write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")

        return str(html_path), str(text_path), str(meta_path)

    def append_jsonl(self, file_obj, record: dict[str, Any]) -> None:
        file_obj.write(json.dumps(record, ensure_ascii=False).encode("utf-8") + b"\n")
        file_obj.flush()

    def append_page(self, record: dict[str, Any]) -> None:
        self.append_jsonl(self.pages_jsonl, record)

    def append_asset(self, record: dict[str, Any]) -> None:
        self.append_jsonl(self.assets_jsonl, record)

    def append_url_map(self, record: dict[str, Any]) -> None:
        self.append_jsonl(self.url_map_jsonl, record)

    def write_run_metadata(self, meta: dict[str, Any]) -> None:
        (self.out_dir / "run.json").write_text(json.dumps(meta, ensure_ascii=False, indent=2), encoding="utf-8")

