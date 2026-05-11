# Models

This folder is intentionally a **placeholder** for local experimentation notes.

With the Docker setup, Ollama stores downloaded models in a Docker volume (see `docker-compose.yml`).

Cost-first usage:
- Keep **many models locally** if you want, but for AWS pick **one** model to minimize disk usage.
- Prefer small, CPU-friendly models and strict output limits.

Suggested workflow:
- Decide the production model id (Ollama model name).
- Run `.\start-local.ps1 -Model <model-id>` locally.
- Once decided, set `OLLAMA_MODEL=<model-id>` on AWS and avoid pulling other models there.

