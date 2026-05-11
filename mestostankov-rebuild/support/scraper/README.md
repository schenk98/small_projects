## scraper/

Goal: crawl `https://www.mestostankov.cz/` and build a structured dataset we can import into the new product.

### Output (actual)

Each run writes to its own folder, e.g. `support/scraper/out/20260508-163500/`:

- `run.json`: run config/metadata
- `raw/pages/<page_id>.html`: raw HTML (one file per page)
- `raw/text/<page_id>.txt`: extracted text (one file per page)
- `raw/meta/<page_id>.json`: fetch + classification metadata
- `index/url_map.jsonl`: mapping of URL → `page_id`
- `normalized/pages.jsonl`: one JSON object per page (normalized, classified)
- `normalized/assets.jsonl`: discovered assets; optional downloaded PDFs stored under `assets/files/`

### Notes

- We’ll prefer respectful crawling: rate limits, retry/backoff, and caching.
- We’ll preserve original URLs and attempt to infer routing patterns like `..._####cs.html`.

### Running (Windows)

From `mestostankov-rebuild/support/scraper/`:

```powershell
.\run.ps1
```

Or run the CLI directly:

```powershell
py -m venv .venv
.\.venv\Scripts\pip install -r requirements.txt
.\.venv\Scripts\python -m playwright install chromium
.\.venv\Scripts\python .\src\stankov_scraper\cli.py crawl --seed "https://www.mestostankov.cz/" --out .\out\test --max-pages 200 --download-assets
```

