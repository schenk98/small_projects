import logging
from difflib import SequenceMatcher
from pathlib import Path

import numpy as np
import pandas as pd

BASE_DIR = Path(__file__).resolve().parent
RATINGS_CSV = "BX-Book-Ratings.csv"
BOOKS_CSV = "BX-Books.csv"
TARGET_TITLE = "the fellowship of the ring (the lord of the rings, part 1)"
TARGET_AUTHOR_SUBSTRING = "tolkien"
BOOK_RATE_THRESHOLD = 8
LOG_LEVEL = logging.DEBUG

logging.basicConfig(level=LOG_LEVEL, format="%(levelname)s: %(message)s")
logger = logging.getLogger(__name__)


def load_data(base_dir: Path) -> tuple[pd.DataFrame, pd.DataFrame]:
    ratings = pd.read_csv(base_dir / RATINGS_CSV, encoding="cp1251", sep=";")
    ratings = ratings[ratings["Book-Rating"] != 0]
    books = pd.read_csv(base_dir / BOOKS_CSV, encoding="cp1251", sep=";", on_bad_lines="skip")
    return ratings, books


def normalize_text_columns(dataframe: pd.DataFrame) -> pd.DataFrame:
    return dataframe.apply(lambda col: col.str.lower() if pd.api.types.is_string_dtype(col) else col)


def resolve_target_title(candidates: list[str], raw_input: str) -> str:
    if not candidates:
        raise ValueError("No candidate titles available for matching.")

    query = raw_input.strip().lower()
    if not query:
        raise ValueError("Target title cannot be empty.")

    exact = next((title for title in candidates if title == query), None)
    if exact is not None:
        return exact

    substring_matches = [title for title in candidates if query in title]
    if substring_matches:
        return min(substring_matches, key=len)

    scored = sorted(
        ((SequenceMatcher(None, query, title).ratio(), title) for title in candidates),
        reverse=True,
        key=lambda item: item[0],
    )
    best_score, best_title = scored[0]
    if best_score < 0.45:
        raise ValueError("No close title match found. Try a longer or more specific title.")
    return best_title


def title_suggestions(
    *,
    query: str,
    top_n: int = 8,
    base_dir: Path = BASE_DIR,
) -> list[dict[str, object]]:
    if top_n < 1:
        raise ValueError("top_n must be >= 1")
    if not query.strip():
        return []

    ratings, books = load_data(base_dir)
    merged_dataset = pd.merge(ratings, books, on=["ISBN"])
    normalized = normalize_text_columns(merged_dataset)
    query_norm = query.strip().lower()

    metadata = (
        normalized.groupby("Book-Title")
        .agg(
            rating_count=("Book-Rating", "count"),
            avg_rating=("Book-Rating", "mean"),
            author=("Book-Author", "first"),
        )
        .reset_index()
    )

    def score_title(title: str) -> float:
        if title.startswith(query_norm):
            return 2.0
        if query_norm in title:
            return 1.0
        return SequenceMatcher(None, query_norm, title).ratio()

    metadata["score"] = metadata["Book-Title"].map(score_title)
    ranked = metadata.sort_values(["score", "rating_count"], ascending=[False, False]).head(top_n)
    return [
        {
            "title": row["Book-Title"],
            "author": row["author"],
            "rating_count": int(row["rating_count"]),
            "avg_rating": float(row["avg_rating"]),
        }
        for _, row in ranked.iterrows()
    ]


def build_recommendation_frame(
    merged_books_ratings: pd.DataFrame,
    target_title: str,
    author_substring: str,
    min_rating_count: int,
) -> tuple[pd.DataFrame, pd.DataFrame, str]:
    normalized = normalize_text_columns(merged_books_ratings)
    author_filter = author_substring.strip().lower()
    if author_filter:
        author_match = normalized["Book-Author"].str.contains(author_filter, na=False)
    else:
        author_match = pd.Series([True] * len(normalized), index=normalized.index)
    candidate_titles = sorted(normalized.loc[author_match, "Book-Title"].dropna().unique().tolist())
    resolved_target_title = resolve_target_title(candidate_titles, target_title)
    title_match = normalized["Book-Title"] == resolved_target_title

    logger.debug("merged dataset shape: %s", merged_books_ratings.shape)
    logger.debug("title exact matches in merged dataset: %s", int(title_match.sum()))
    logger.debug("title+author matches in merged dataset: %s", int((title_match & author_match).sum()))

    target_readers = np.unique(normalized["User-ID"][title_match & author_match].tolist())
    logger.debug("unique target readers: %s", len(target_readers))

    books_of_target_readers = normalized[normalized["User-ID"].isin(target_readers)]
    logger.debug("books_of_target_readers shape: %s", books_of_target_readers.shape)
    logger.debug("non-null ISBN in books_of_target_readers: %s", int(books_of_target_readers["ISBN"].notna().sum()))
    logger.debug("non-null Publisher in books_of_target_readers: %s", int(books_of_target_readers["Publisher"].notna().sum()))

    ratings_per_book = books_of_target_readers.groupby(["Book-Title"]).agg("count").reset_index()
    logger.debug("unique titles after target user filter: %s", ratings_per_book.shape[0])

    books_to_compare = ratings_per_book["Book-Title"][ratings_per_book["User-ID"] >= min_rating_count].tolist()
    logger.debug("titles passing threshold: %s", len(books_to_compare))
    logger.debug("target title present in books_to_compare: %s", resolved_target_title in books_to_compare)

    ratings_data = books_of_target_readers[["User-ID", "Book-Rating", "Book-Title"]][
        books_of_target_readers["Book-Title"].isin(books_to_compare)
    ]
    logger.debug("ratings_data shape: %s", ratings_data.shape)

    ratings_data_nodup = ratings_data.groupby(["User-ID", "Book-Title"])["Book-Rating"].mean().to_frame().reset_index()
    dataset_for_corr = ratings_data_nodup.pivot(index="User-ID", columns="Book-Title", values="Book-Rating")
    logger.debug("dataset_for_corr shape: %s", dataset_for_corr.shape)
    logger.debug("target title present in pivot columns: %s", resolved_target_title in dataset_for_corr.columns)
    return dataset_for_corr, ratings_data, resolved_target_title


def compute_correlations(target_title: str, dataset_for_corr: pd.DataFrame, ratings_data: pd.DataFrame) -> pd.DataFrame:
    dataset_of_other_books = dataset_for_corr.copy(deep=False)
    dataset_of_other_books.drop([target_title], axis=1, inplace=True)

    book_titles: list[str] = []
    correlations: list[float] = []
    avg_ratings: list[float] = []
    rating_counts: list[int] = []

    metrics_by_title = (
        ratings_data.groupby("Book-Title")["Book-Rating"]
        .agg(avg_rating="mean", rating_count="count")
        .to_dict("index")
    )

    for book_title in list(dataset_of_other_books.columns.values):
        book_titles.append(book_title)
        correlations.append(dataset_for_corr[target_title].corr(dataset_of_other_books[book_title]))
        metrics = metrics_by_title.get(book_title, {"avg_rating": np.nan, "rating_count": 0})
        avg_ratings.append(float(metrics["avg_rating"]))
        rating_counts.append(int(metrics["rating_count"]))

    correlation_frame = pd.DataFrame(
        list(zip(book_titles, correlations, avg_ratings, rating_counts)),
        columns=["book", "corr", "avg_rating", "rating_count"],
    )
    return correlation_frame


def generate_recommendations(
    *,
    target_title: str,
    target_author_substring: str,
    rating_threshold: int,
    top_n: int = 10,
    base_dir: Path = BASE_DIR,
) -> tuple[pd.DataFrame, str]:
    if rating_threshold < 1:
        raise ValueError("rating_threshold must be >= 1")
    if top_n < 1:
        raise ValueError("top_n must be >= 1")

    ratings, books = load_data(base_dir)
    logger.debug("ratings shape: %s", ratings.shape)
    logger.debug("books shape: %s", books.shape)

    merged_dataset = pd.merge(ratings, books, on=["ISBN"])
    dataset_for_corr, ratings_data, resolved_target_title = build_recommendation_frame(
        merged_dataset,
        target_title,
        target_author_substring,
        rating_threshold,
    )

    if resolved_target_title not in dataset_for_corr.columns:
        raise ValueError(
            "Target title is not present after filtering. "
            "Try lowering threshold or verify title/author values."
        )

    correlation_frame = compute_correlations(resolved_target_title, dataset_for_corr, ratings_data)
    top_recommendations = correlation_frame.sort_values("corr", ascending=False).head(top_n)
    return top_recommendations, resolved_target_title


def main() -> None:
    ratings, books = load_data(BASE_DIR)
    logger.debug("ratings shape: %s", ratings.shape)
    logger.debug("books shape: %s", books.shape)

    merged_dataset = pd.merge(ratings, books, on=["ISBN"])
    dataset_for_corr, ratings_data, resolved_target_title = build_recommendation_frame(
        merged_dataset,
        TARGET_TITLE,
        TARGET_AUTHOR_SUBSTRING,
        BOOK_RATE_THRESHOLD,
    )

    correlation_frame = compute_correlations(resolved_target_title, dataset_for_corr, ratings_data)
    top_recommendations = correlation_frame.sort_values("corr", ascending=False).head(10)

    logger.info("Correlation for book: %s", resolved_target_title)
    logger.info("Top recommendations:")
    print(top_recommendations.to_string(index=False))


if __name__ == "__main__":
    main()

