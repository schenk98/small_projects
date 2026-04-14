from pathlib import Path
import logging

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


def build_recommendation_frame(
    merged_books_ratings: pd.DataFrame,
    target_title: str,
    author_substring: str,
    min_rating_count: int,
) -> tuple[pd.DataFrame, pd.DataFrame]:
    normalized = normalize_text_columns(merged_books_ratings)
    title_match = normalized["Book-Title"] == target_title
    author_match = normalized["Book-Author"].str.contains(author_substring, na=False)

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
    logger.debug("target title present in books_to_compare: %s", target_title in books_to_compare)

    ratings_data = books_of_target_readers[["User-ID", "Book-Rating", "Book-Title"]][
        books_of_target_readers["Book-Title"].isin(books_to_compare)
    ]
    logger.debug("ratings_data shape: %s", ratings_data.shape)

    ratings_data_nodup = ratings_data.groupby(["User-ID", "Book-Title"])["Book-Rating"].mean().to_frame().reset_index()
    dataset_for_corr = ratings_data_nodup.pivot(index="User-ID", columns="Book-Title", values="Book-Rating")
    logger.debug("dataset_for_corr shape: %s", dataset_for_corr.shape)
    logger.debug("target title present in pivot columns: %s", target_title in dataset_for_corr.columns)
    return dataset_for_corr, ratings_data


def compute_correlations(target_title: str, dataset_for_corr: pd.DataFrame, ratings_data: pd.DataFrame) -> pd.DataFrame:
    dataset_of_other_books = dataset_for_corr.copy(deep=False)
    dataset_of_other_books.drop([target_title], axis=1, inplace=True)

    book_titles: list[str] = []
    correlations: list[float] = []
    avg_ratings: list[float] = []

    for book_title in list(dataset_of_other_books.columns.values):
        book_titles.append(book_title)
        correlations.append(dataset_for_corr[target_title].corr(dataset_of_other_books[book_title]))
        avg_rating = ratings_data[ratings_data["Book-Title"] == book_title].groupby("Book-Title")["Book-Rating"].mean()
        avg_ratings.append(avg_rating.min())

    correlation_frame = pd.DataFrame(
        list(zip(book_titles, correlations, avg_ratings)),
        columns=["book", "corr", "avg_rating"],
    )
    return correlation_frame


def main() -> None:
    ratings, books = load_data(BASE_DIR)
    logger.debug("ratings shape: %s", ratings.shape)
    logger.debug("books shape: %s", books.shape)

    merged_dataset = pd.merge(ratings, books, on=["ISBN"])
    dataset_for_corr, ratings_data = build_recommendation_frame(
        merged_dataset,
        TARGET_TITLE,
        TARGET_AUTHOR_SUBSTRING,
        BOOK_RATE_THRESHOLD,
    )

    correlation_frame = compute_correlations(TARGET_TITLE, dataset_for_corr, ratings_data)
    top_recommendations = correlation_frame.sort_values("corr", ascending=False).head(10)

    logger.info("Correlation for book: %s", TARGET_TITLE)
    logger.info("Top recommendations:")
    print(top_recommendations.to_string(index=False))


if __name__ == "__main__":
    main()

