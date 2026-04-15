# Book Recommender Web App

Web app with:
- Litestar backend (`app.py`)
- Minimal TypeScript/HTML/CSS frontend (`frontend/*`)
- Data preparation pipeline (`prepare_data.py`)
- Recommendation engine (`book_rec.py`)

## Current Functionality

- Download + clean source dataset from Kaggle using a button in the UI.
- Keep cleaned CSVs (`BX-Books.csv`, `BX-Book-Ratings.csv`) in project folder.
- Request recommendations via API and display them in a table in browser.
- Use typo-tolerant title resolution (nearest title match from dataset).
- Get live title suggestions in GUI based on dataset content.
- Run fully in Docker on Windows.

## Architecture

```mermaid
flowchart TD
    FrontendWebUI["Frontend Web UI (HTML + TypeScript + CSS)"] --> BackendApi["Litestar Backend API (app.py)"]
    BackendApi --> DataPreparationModule["Dataset Preparation Module (prepare_data.py)"]
    BackendApi --> RecommendationEngine["Recommendation Engine (book_rec.py)"]
    DataPreparationModule --> CleanedBooksCsv["Cleaned Books Dataset (BX-Books.csv)"]
    DataPreparationModule --> CleanedRatingsCsv["Cleaned Ratings Dataset (BX-Book-Ratings.csv)"]
    CleanedBooksCsv --> RecommendationEngine
    CleanedRatingsCsv --> RecommendationEngine
    RecommendationEngine --> BackendApi
    BackendApi --> FrontendWebUI
```

## API Endpoints

- `GET /` -> serves web UI
- `GET /api/health` -> health check
- `POST /api/prepare-data` -> runs Kaggle download + ETL
- `POST /api/recommend` -> returns recommendations, including matched title and metrics
- `GET /api/title-suggestions?q=<query>&top_n=<n>` -> returns title suggestions from dataset

Example request body for `POST /api/recommend`:

```json
{
  "target_title": "the fellowship of the ring (the lord of the rings, part 1)",
  "target_author_substring": "tolkien",
  "rating_threshold": 8,
  "top_n": 10
}
```

Example response body for `POST /api/recommend`:

```json
{
  "ok": true,
  "matched_title": "the fellowship of the ring (the lord of the rings, part 1)",
  "items": [
    {
      "book": "the two towers (the lord of the rings, part 2)",
      "corr": 0.81,
      "avg_rating": 8.74,
      "rating_count": 129
    }
  ]
}
```

Example response body for `GET /api/title-suggestions`:

```json
{
  "ok": true,
  "items": [
    {
      "title": "harry potter and the chamber of secrets (book 2)",
      "author": "j. k. rowling",
      "rating_count": 273,
      "avg_rating": 8.12
    }
  ]
}
```

## Recommendation Metrics (Important)

- `rating_threshold` currently means **minimum rating count** (number of ratings/users for a title in the comparison set), not minimum average score.
- `avg_rating` is the mean score for each recommended title and can be below 8 even when `rating_threshold = 8`.
- `corr` is Pearson correlation between target book and candidate book ratings.
- `rating_count` is a popularity/reliability metric (how many ratings contributed for that title in the filtered set).

## `prepare_data.py` Flow

1. Authenticate with Kaggle (`kaggle.json`).
2. Download dataset archive.
3. Extract source CSV files.
4. Clean books data:
   - normalize text/ISBN
   - parse year
   - remove invalid rows
   - deduplicate by `ISBN`
5. Clean ratings data:
   - normalize ISBN
   - parse numeric fields
   - enforce rating range `[0, 10]`
   - deduplicate by `(User-ID, ISBN)`
6. Save cleaned output files.
7. Remove temporary download folder (unless `--keep-downloads`).

```mermaid
flowchart TD
    PrepareDataScript["prepare_data.py"] --> KaggleAuthStep["Authenticate with Kaggle API"]
    KaggleAuthStep --> DownloadArchiveStep["Download Dataset ZIP"]
    DownloadArchiveStep --> ExtractCsvStep["Extract Source CSV Files"]
    ExtractCsvStep --> CleanBooksStep["Clean and Normalize Books Data"]
    ExtractCsvStep --> CleanRatingsStep["Clean and Normalize Ratings Data"]
    CleanBooksStep --> SaveBooksOutput["Write BX-Books.csv"]
    CleanRatingsStep --> SaveRatingsOutput["Write BX-Book-Ratings.csv"]
    SaveBooksOutput --> CleanupTempStep["Remove Temporary Download Directory"]
    SaveRatingsOutput --> CleanupTempStep
```

## Local Run (without Docker)

```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

Open [http://localhost:8000](http://localhost:8000).

## Docker Run on Windows

### 1) Install Docker Desktop

1. Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).
2. Enable WSL2 integration during setup.
3. Start Docker Desktop and wait until it shows "Engine running".

### 2) Verify Docker in PowerShell

```powershell
docker --version
docker compose version
```

### 3) Kaggle credentials

- Put credentials at: `C:\Users\<you>\.kaggle\kaggle.json`
- JSON format:

```json
{"username":"your_kaggle_username","key":"your_kaggle_api_key"}
```

### 4) Build and run app

From `book_recommender` folder:

```powershell
docker compose build
docker compose up
```

Then open [http://localhost:8000](http://localhost:8000).

### 5) Stop app

```powershell
docker compose down
```

### 6) Rebuild after code changes

If backend or frontend files change:

```powershell
docker compose down
docker compose up --build
```

## Configurable values

In `book_rec.py`:
- `TARGET_TITLE`
- `TARGET_AUTHOR_SUBSTRING`
- `BOOK_RATE_THRESHOLD`
- `LOG_LEVEL`

In `prepare_data.py`:
- `DATASET_SLUG`
- `LOG_LEVEL`
