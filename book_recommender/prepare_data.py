from __future__ import annotations

import argparse
import logging
import shutil
from pathlib import Path
from zipfile import ZipFile

import pandas as pd

DATASET_SLUG = "arashnic/book-recommendation-dataset"
OUTPUT_BOOKS = "BX-Books.csv"
OUTPUT_RATINGS = "BX-Book-Ratings.csv"

LOG_LEVEL = logging.INFO
logging.basicConfig(level=LOG_LEVEL, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)


def log_row_delta(step: str, before_count: int, after_count: int) -> None:
    removed = before_count - after_count
    logger.debug("%s -> rows: %s -> %s (removed: %s)", step, before_count, after_count, removed)


def normalize_isbn(series: pd.Series) -> pd.Series:
    return (
        series.astype(str)
        .str.upper()
        .str.strip()
        .str.replace(r"[^0-9X]", "", regex=True)
    )


def normalize_text(series: pd.Series) -> pd.Series:
    return (
        series.astype(str)
        .str.replace(r"\s+", " ", regex=True)
        .str.strip()
    )


def download_kaggle_zip(target_dir: Path, dataset_slug: str) -> Path:
    from kaggle.api.kaggle_api_extended import KaggleApi

    target_dir.mkdir(parents=True, exist_ok=True)
    api = KaggleApi()
    try:
        api.authenticate()
    except Exception as exc:
        logger.error("Kaggle authentication failed.")
        logger.error("Expected credentials in ~/.kaggle/kaggle.json or KAGGLE_CONFIG_DIR.")
        raise RuntimeError("Kaggle authentication failed. Check kaggle.json path/contents.") from exc
    logger.info("Downloading dataset: %s", dataset_slug)
    try:
        api.dataset_download_files(dataset_slug, path=str(target_dir), unzip=False, force=True)
    except Exception as exc:
        logger.error("Kaggle download failed for dataset: %s", dataset_slug)
        raise RuntimeError("Kaggle download failed.") from exc

    zip_path = target_dir / f"{dataset_slug.split('/')[-1]}.zip"
    if not zip_path.exists():
        candidates = sorted(target_dir.glob("*.zip"))
        if not candidates:
            raise FileNotFoundError("No Kaggle zip file found after download.")
        zip_path = candidates[-1]
    if zip_path.stat().st_size == 0:
        raise RuntimeError(f"Downloaded zip is empty: {zip_path}")
    logger.info("Downloaded archive: %s", zip_path)
    return zip_path


def extract_zip(zip_path: Path, target_dir: Path) -> None:
    logger.info("Extracting: %s", zip_path.name)
    try:
        with ZipFile(zip_path, "r") as zf:
            zf.extractall(target_dir)
    except Exception as exc:
        logger.error("Zip extraction failed for: %s", zip_path)
        raise RuntimeError("Failed to extract downloaded zip.") from exc
    extracted_files = sorted([p.name for p in target_dir.glob("*.csv")])
    logger.info("Extracted CSV files: %s", ", ".join(extracted_files) if extracted_files else "none")
    if not extracted_files:
        logger.warning("No CSV files found after extraction.")


def clean_ratings(raw_ratings: pd.DataFrame) -> pd.DataFrame:
    ratings = raw_ratings.copy()
    logger.debug("Ratings raw shape: %s", ratings.shape)
    if ratings.empty:
        logger.warning("Ratings input is empty before cleaning.")
    ratings = ratings.rename(columns={"UserID": "User-ID", "ISBN": "ISBN", "BookRating": "Book-Rating"})
    required = {"User-ID", "ISBN", "Book-Rating"}
    ordered_required = ["User-ID", "ISBN", "Book-Rating"]
    if not required.issubset(ratings.columns):
        raise ValueError(f"Ratings file missing required columns: {required}")

    ratings = ratings[ordered_required].copy()
    logger.debug("Ratings selected required columns: %s", list(ratings.columns))
    ratings["User-ID"] = pd.to_numeric(ratings["User-ID"], errors="coerce")
    ratings["ISBN"] = normalize_isbn(ratings["ISBN"])
    ratings["Book-Rating"] = pd.to_numeric(ratings["Book-Rating"], errors="coerce")

    before = len(ratings)
    ratings = ratings.dropna(subset=["User-ID", "ISBN", "Book-Rating"])
    log_row_delta("Ratings drop rows with missing required values", before, len(ratings))

    before = len(ratings)
    ratings = ratings[(ratings["Book-Rating"] >= 0) & (ratings["Book-Rating"] <= 10)]
    log_row_delta("Ratings keep score range [0, 10]", before, len(ratings))

    before = len(ratings)
    ratings = ratings[ratings["ISBN"] != ""]
    log_row_delta("Ratings remove empty ISBN", before, len(ratings))

    ratings["User-ID"] = ratings["User-ID"].astype("int64")
    ratings["Book-Rating"] = ratings["Book-Rating"].astype("int64")
    before = len(ratings)
    ratings = ratings.drop_duplicates(subset=["User-ID", "ISBN"], keep="last")
    log_row_delta("Ratings deduplicate by (User-ID, ISBN)", before, len(ratings))
    logger.info("Ratings cleaned shape: %s", ratings.shape)
    if ratings.empty:
        logger.warning("Ratings became empty after cleaning.")
    return ratings


def clean_books(raw_books: pd.DataFrame) -> pd.DataFrame:
    books = raw_books.copy()
    logger.debug("Books raw shape: %s", books.shape)
    if books.empty:
        logger.warning("Books input is empty before cleaning.")
    rename_map = {
        "ISBN": "ISBN",
        "BookTitle": "Book-Title",
        "BookAuthor": "Book-Author",
        "YearOfPublication": "Year-Of-Publication",
        "Publisher": "Publisher",
        "ImageURLS": "Image-URL-S",
        "ImageURLM": "Image-URL-M",
        "ImageURLL": "Image-URL-L",
    }
    books = books.rename(columns=rename_map)
    required = {"ISBN", "Book-Title", "Book-Author"}
    if not required.issubset(books.columns):
        raise ValueError(f"Books file missing required columns: {required}")

    for column in ["Publisher", "Image-URL-S", "Image-URL-M", "Image-URL-L", "Year-Of-Publication"]:
        if column not in books.columns:
            books[column] = ""
            logger.debug("Books missing '%s' column, created empty fallback.", column)

    cols = [
        "ISBN",
        "Book-Title",
        "Book-Author",
        "Year-Of-Publication",
        "Publisher",
        "Image-URL-S",
        "Image-URL-M",
        "Image-URL-L",
    ]
    books = books[cols].copy()
    logger.debug("Books standardized columns: %s", cols)

    before = len(books)
    books["ISBN"] = normalize_isbn(books["ISBN"])
    books["Book-Title"] = normalize_text(books["Book-Title"])
    books["Book-Author"] = normalize_text(books["Book-Author"])
    books["Publisher"] = normalize_text(books["Publisher"])
    books["Year-Of-Publication"] = pd.to_numeric(books["Year-Of-Publication"], errors="coerce").fillna(0).astype("int64")
    logger.debug("Books normalized text/year fields for %s rows", before)

    before = len(books)
    books = books.dropna(subset=["ISBN", "Book-Title", "Book-Author"])
    log_row_delta("Books drop rows with missing required values", before, len(books))

    before = len(books)
    books = books[(books["ISBN"] != "") & (books["Book-Title"] != "") & (books["Book-Author"] != "")]
    log_row_delta("Books remove empty ISBN/title/author", before, len(books))

    before = len(books)
    books = books.drop_duplicates(subset=["ISBN"], keep="first")
    log_row_delta("Books deduplicate by ISBN", before, len(books))
    logger.info("Books cleaned shape: %s", books.shape)
    if books.empty:
        logger.warning("Books became empty after cleaning.")
    return books


def save_outputs(clean_books_df: pd.DataFrame, clean_ratings_df: pd.DataFrame, output_dir: Path) -> None:
    books_path = output_dir / OUTPUT_BOOKS
    ratings_path = output_dir / OUTPUT_RATINGS

    if books_path.exists():
        logger.warning("Overwriting existing file: %s", books_path)
    if ratings_path.exists():
        logger.warning("Overwriting existing file: %s", ratings_path)

    clean_books_df.to_csv(books_path, sep=";", index=False, encoding="cp1251", errors="replace")
    clean_ratings_df.to_csv(ratings_path, sep=";", index=False, encoding="cp1251", errors="replace")
    logger.info("Saved cleaned books: %s (%s rows, %s bytes)", books_path.name, len(clean_books_df), books_path.stat().st_size)
    logger.info(
        "Saved cleaned ratings: %s (%s rows, %s bytes)",
        ratings_path.name,
        len(clean_ratings_df),
        ratings_path.stat().st_size,
    )


def prepare_dataset(work_dir: Path, keep_downloads: bool) -> None:
    downloads_dir = work_dir / "_downloads"
    logger.info("Preparing dataset in directory: %s", work_dir)
    zip_path = download_kaggle_zip(downloads_dir, DATASET_SLUG)
    extract_zip(zip_path, downloads_dir)

    books_file = downloads_dir / "Books.csv"
    ratings_file = downloads_dir / "Ratings.csv"
    if not books_file.exists() or not ratings_file.exists():
        raise FileNotFoundError("Expected Books.csv and Ratings.csv in downloaded dataset.")
    logger.info("Found source files: %s and %s", books_file.name, ratings_file.name)

    raw_books = pd.read_csv(books_file, low_memory=False)
    raw_ratings = pd.read_csv(ratings_file, low_memory=False)
    cleaned_books = clean_books(raw_books)
    cleaned_ratings = clean_ratings(raw_ratings)
    save_outputs(cleaned_books, cleaned_ratings, work_dir)

    if not keep_downloads and downloads_dir.exists():
        try:
            shutil.rmtree(downloads_dir)
            logger.info("Removed temporary download directory.")
        except Exception as exc:
            logger.warning("Failed to remove temporary directory: %s", exc)
    else:
        logger.info("Kept temporary download directory: %s", downloads_dir)


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description="Download and clean book recommender dataset from Kaggle.")
    parser.add_argument("--keep-downloads", action="store_true", help="Keep temporary downloaded zip and extracted files.")
    return parser.parse_args()


def main() -> None:
    args = parse_args()
    prepare_dataset(Path(__file__).resolve().parent, keep_downloads=args.keep_downloads)


if __name__ == "__main__":
    main()
