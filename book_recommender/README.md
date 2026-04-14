# Book Recommender

Simple collaborative filtering script based on user rating correlations.

## Setup

```bash
pip install -r requirements.txt
```

## Run

```bash
python book_rec.py
```

## Notes

- Keep `BX-Book-Ratings.csv` and `BX-Books.csv` in this folder.
- Change `TARGET_TITLE` in `book_rec.py` to generate recommendations for another seed book.
- Control verbosity with `LOG_LEVEL` (`DEBUG`, `INFO`, `WARNING`).
