# Book Recommender Web App

A Dockerized book recommendation system with a Litestar API, simple web UI, Kaggle-based data preparation, typo-tolerant title matching, and AWS EC2 deployment support.

**Live Demo (AWS EC2):** [http://13.48.193.19](http://13.48.193.19)

---

## System Overview

This section explains what the system does and how its parts fit together. Deployment steps are in the next section.

### What It Does

- Downloads and cleans the source dataset from Kaggle.
- Stores cleaned files as `BX-Books.csv` and `BX-Book-Ratings.csv`.
- Serves a browser UI for:
  - preparing data,
  - searching titles with live suggestions,
  - requesting recommendations.
- Generates recommendations with:
  - nearest title resolution (for typos/inexact input),
  - correlation score,
  - average rating,
  - rating count.
- Provides a helper suggestion table (title, author, ratings count, average rating, ISBN) with click-to-prefill of title + ISBN + author.

### Core Components

- Backend API: `app.py` (Litestar)
- Recommendation engine: `book_rec.py`
- Data preparation pipeline: `prepare_data.py`
- Frontend: `frontend/index.html`, `frontend/app.js`, `frontend/styles.css`

### High-Level Architecture

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



### API Surface (Quick)

- `GET /` - serves web UI
- `GET /api/health` - health check
- `POST /api/prepare-data` - runs Kaggle download + ETL
- `POST /api/recommend` - returns recommendations, matched title, and metrics
- `GET /api/title-suggestions?q=<query>&top_n=<n>` - returns title suggestions from dataset

**API Examples**

Request body for `POST /api/recommend`:

```json
{
  "target_title": "the fellowship of the ring (the lord of the rings, part 1)",
  "target_isbn": "0345339703",
  "target_author_substring": "tolkien",
  "rating_threshold": 8,
  "top_n": 10
}
```

Response body for `POST /api/recommend`:

```json
{
  "ok": true,
  "matched_title": "the fellowship of the ring (the lord of the rings, part 1)",
  "matched_isbn": "0345339703",
  "total_candidates": 250,
  "returned_count": 10,
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

Response body for `GET /api/title-suggestions`:

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



### Recommendation Metrics (Important)

- `rating_threshold` means minimum rating count (how many ratings/users are required in comparison set), not minimum average score.
- `avg_rating` is the average score for a recommended title.
- `corr` is Pearson correlation between target book and candidate book.
- `rating_count` is a confidence/popularity proxy showing how many ratings contributed.
- `total_candidates` is the number of recommendation candidates before applying `top_n`.
- `returned_count` is the number actually returned after `top_n` limit.

### Data Preparation Flow

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



**Detailed ETL Steps**

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



---

## Run And Deploy

This section is operational: local run, Docker run on Windows, and AWS EC2 deployment.

### Local Run (Without Docker)

```bash
pip install -r requirements.txt
uvicorn app:app --host 0.0.0.0 --port 8000
```

Open [http://localhost:8000](http://localhost:8000).

### Docker Run On Windows (Primary Local Path)

Quick path:

```powershell
docker compose build
docker compose up
```

Open [http://localhost:8000](http://localhost:8000).

**Detailed Windows Docker Setup**

1. Install [Docker Desktop for Windows](https://www.docker.com/products/docker-desktop/).
2. Enable WSL2 integration during setup.
3. Start Docker Desktop and wait until it shows "Engine running".

Verify Docker:

```powershell
docker --version
docker compose version
```

Set Kaggle credentials file:

- Path: `C:\Users\<you>\.kaggle\kaggle.json`
- Format:

```json
{"username":"your_kaggle_username","key":"your_kaggle_api_key"}
```

Run from `book_recommender`:

```powershell
docker compose build
docker compose up
```

Stop app:

```powershell
docker compose down
```

Rebuild after code changes:

```powershell
docker compose down
docker compose up --build
```



### Deploy To AWS EC2 (Docker + Secrets Manager)

The AWS deployment uses `docker-compose.aws.yml` with AWS Secrets Manager for credentials.

#### Security Model (Short)

- Kaggle credentials are not stored in code, image, or compose env values.
- Credentials live in AWS Secrets Manager.
- EC2 fetches secret into `./secrets/kaggle.json`.
- Container mounts that file read-only to `/root/.kaggle/kaggle.json`.

**Step-by-Step AWS Deployment**

1. Create secret in AWS Secrets Manager:
  - Service: Secrets Manager -> Store a new secret
  - Type: Other type of secret
  - Value:
  - Secret name: `book-recommender/kaggle`
2. Create IAM role for EC2:
  - Allow `secretsmanager:GetSecretValue` on `book-recommender/kaggle`
  - Attach role to EC2 instance profile
3. Launch EC2:
  - Ubuntu 22.04 or 24.04
  - Free-tier eligible instance (`t2.micro` or equivalent)
  - Inbound rules:
    - `SSH` from My IP
    - `HTTP` from `0.0.0.0/0`
4. Connect from local machine:
  ```bash
   ssh -i /path/to/keypair.pem ubuntu@<EC2_PUBLIC_IP>
  ```
5. Install runtime dependencies:
  ```bash
   sudo apt update
   sudo apt install -y docker.io docker-compose-v2 git unzip
   curl "https://awscli.amazonaws.com/awscli-exe-linux-x86_64.zip" -o "awscliv2.zip"
   unzip awscliv2.zip
   sudo ./aws/install
   aws --version
  ```
6. Upload project:
  - Option A: `git clone <your-repo-url>`
  - Option B: `scp` from local machine
7. Fetch Kaggle secret on EC2:
  ```bash
   chmod +x scripts/fetch_kaggle_secret.sh
   ./scripts/fetch_kaggle_secret.sh book-recommender/kaggle ./secrets/kaggle.json
  ```
8. Build and run:
  ```bash
   docker compose -f docker-compose.aws.yml up -d --build
   docker ps
   curl http://localhost/api/health
  ```
9. Access app:
  - `http://<EC2_PUBLIC_IP>`



**AWS Operations Commands**

```bash
docker compose -f docker-compose.aws.yml logs -f
docker compose -f docker-compose.aws.yml restart
docker compose -f docker-compose.aws.yml down
```



---

## Security Notes

- Keep private key files (`*.pem`) outside repository when possible.
- `secrets/` is git-ignored and docker-ignored to prevent accidental leaks.
- Never commit `kaggle.json`, `.env`, or any credential file.

---

## Configurable Values

In `book_rec.py`:

- `TARGET_TITLE`
- `TARGET_AUTHOR_SUBSTRING`
- `BOOK_RATE_THRESHOLD`
- `LOG_LEVEL`

In `prepare_data.py`:

- `DATASET_SLUG`
- `LOG_LEVEL`

## Possible improvements

- Performance
  - Migrate from CSV-based reads to Postgres.
  - Add indexes for book_title, book_author, and normalized search columns.
- Search quality
  - Improve fuzzy title matching and ranking.
  - Add sequel/series-aware suggestions (not only substring; use token similarity + series metadata where possible).
  - Make author filter optional; allow title-only discovery and author-first browsing.
- Caching layer
  - Add caching to suggesting algorithm to reduce latency (if needed after csv -> postgres migration)
- Data refresh strategy
  - Add scheduled data refresh (manual + cron-like trigger) instead of only button-triggered prep.
  - Move refreshing job into background and adjust web page
- Recommendation quality
  - Add hybrid ranking (correlation + avg rating + rating count weighting).
  - Add diversity filter to avoid near-duplicate recommendations (as optional toggle).
- API
  - App could be improved to work completely with API calls
  - Current API calls are insufficiently logging activity and not enough edge cases are covered
- Edge cases
  - There are definitively missed edgecases, that were overlooked in somewhat rushed implementation
- Security 
  - Before software can be moved into profesional production, the security should be discussed with someone specializing in that
- Testing
  - Lack of unit and smoke tests for full functionality
- Sessions, ukládání výsledků, fronta požadavků
  - Zatím není řešeno frontování požadavků, sessions and ukládání výsledků, není tedy dost dobře možné, aby více uživatelů najednou používalo systém (resp. to může vést k podivnému chování)

