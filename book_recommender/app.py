from __future__ import annotations

import logging
from dataclasses import asdict, dataclass
from pathlib import Path

from litestar import Litestar, post, get
from litestar.config.cors import CORSConfig
from litestar.response import Response
from litestar.static_files import create_static_files_router

from book_rec import (
    BOOK_RATE_THRESHOLD,
    TARGET_AUTHOR_SUBSTRING,
    TARGET_TITLE,
    generate_recommendations,
    title_suggestions,
)
from prepare_data import prepare_dataset

BASE_DIR = Path(__file__).resolve().parent
STATIC_DIR = BASE_DIR / "frontend"

LOG_LEVEL = logging.INFO
logging.basicConfig(level=LOG_LEVEL, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)


@dataclass
class RecommendationRequest:
    target_title: str = TARGET_TITLE
    target_isbn: str | None = None
    target_author_substring: str = TARGET_AUTHOR_SUBSTRING
    rating_threshold: int = BOOK_RATE_THRESHOLD
    top_n: int = 10


@dataclass
class RecommendationItem:
    book: str
    corr: float
    avg_rating: float
    rating_count: int


@dataclass
class ApiMessage:
    ok: bool
    message: str


@get("/")
async def root() -> Response[str]:
    """Serve the frontend entry page."""
    index_html = (STATIC_DIR / "index.html").read_text(encoding="utf-8")
    return Response(content=index_html, media_type="text/html")


@get("/api/health")
async def health() -> dict[str, str]:
    """Return a lightweight health status."""
    return {"status": "ok"}


@post("/api/recommend")
async def recommend(data: RecommendationRequest) -> dict[str, object]:
    """Generate recommendations for the requested title."""
    logger.info("Recommendation request for title: %s", data.target_title)
    try:
        frame, resolved_target_title, resolved_target_isbn, total_candidates = generate_recommendations(
            target_title=data.target_title.lower(),
            target_isbn=(data.target_isbn or "").strip() or None,
            target_author_substring=data.target_author_substring.lower(),
            rating_threshold=data.rating_threshold,
            top_n=data.top_n,
        )
    except Exception as exc:
        logger.error("Recommendation failed: %s", exc)
        return {"ok": False, "message": str(exc), "items": []}

    items = [RecommendationItem(**row) for row in frame.to_dict(orient="records")]
    return {
        "ok": True,
        "matched_title": resolved_target_title,
        "matched_isbn": resolved_target_isbn,
        "total_candidates": total_candidates,
        "returned_count": len(items),
        "items": [asdict(item) for item in items],
    }


@get("/api/title-suggestions")
async def suggest_titles(q: str = "", top_n: int = 8) -> dict[str, object]:
    """Return ranked title suggestions for autocomplete."""
    try:
        suggestions = title_suggestions(query=q, top_n=top_n, base_dir=BASE_DIR)
    except Exception as exc:
        logger.error("Title suggestion failed: %s", exc)
        return {"ok": False, "message": str(exc), "items": []}
    return {"ok": True, "items": suggestions}


@post("/api/prepare-data")
async def trigger_prepare_data() -> ApiMessage:
    """Run data download and cleaning pipeline."""
    logger.info("Manual prepare-data trigger called.")
    try:
        prepare_dataset(BASE_DIR, keep_downloads=False)
    except Exception as exc:
        logger.error("Prepare-data failed: %s", exc)
        return ApiMessage(ok=False, message=f"Data preparation failed: {exc}")
    return ApiMessage(ok=True, message="Data preparation finished successfully.")


app = Litestar(
    route_handlers=[
        root,
        health,
        recommend,
        suggest_titles,
        trigger_prepare_data,
        create_static_files_router(path="/static", directories=[STATIC_DIR]),
    ],
    cors_config=CORSConfig(allow_origins=["*"]),
)
