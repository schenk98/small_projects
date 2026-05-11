from __future__ import annotations

import argparse
import asyncio
from pathlib import Path
import sys

if __package__ in (None, ""):
    # Allow running as a script:
    #   python src/stankov_scraper/cli.py ...
    # by ensuring `src/` is on sys.path so absolute imports work.
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))

from stankov_scraper.crawler import crawl as crawl_impl
from stankov_scraper.models import CrawlConfig


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Crawl mestostankov.cz into a structured dataset.")
    parser.add_argument("--seed", required=True, help="Seed URL to start crawling from.")
    parser.add_argument("--out", required=True, help="Output directory for this run.")
    parser.add_argument("--allowed-domain", action="append", default=None, help="Allowed domain(s). Repeatable.")
    parser.add_argument("--max-pages", type=int, default=2500)
    parser.add_argument("--concurrency", type=int, default=8)
    parser.add_argument("--delay-ms", type=int, default=200)
    parser.add_argument("--download-assets", action="store_true")
    parser.add_argument("--no-browser-fallback", action="store_true", help="Disable Playwright fallback.")

    args = parser.parse_args()

    Path(args.out).mkdir(parents=True, exist_ok=True)
    cfg = CrawlConfig(
        seed=args.seed,
        out=args.out,
        allowed_domains=args.allowed_domain or ["www.mestostankov.cz", "mestostankov.cz"],
        max_pages=args.max_pages,
        concurrency=args.concurrency,
        delay_ms=args.delay_ms,
        download_assets=args.download_assets,
        use_browser_fallback=not args.no_browser_fallback,
    )
    asyncio.run(crawl_impl(cfg))

