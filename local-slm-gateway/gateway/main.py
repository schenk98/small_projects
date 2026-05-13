import os
import time
from typing import Literal

import httpx
from fastapi import FastAPI, Header, HTTPException
from fastapi import Request
from pydantic import BaseModel, Field


def _require_env(name: str) -> str:
    v = os.getenv(name)
    if not v:
        raise RuntimeError(f"Missing required env var: {name}")
    return v


OLLAMA_BASE_URL = os.getenv("OLLAMA_BASE_URL", "http://localhost:11434").rstrip("/")
OLLAMA_MODEL = os.getenv("OLLAMA_MODEL", "phi4-mini")
API_KEY = _require_env("AI_GATEWAY_API_KEY")

MAX_PROMPT_CHARS = int(os.getenv("AI_MAX_PROMPT_CHARS", "1200"))
MAX_TURNS = int(os.getenv("AI_MAX_TURNS", "8"))
MAX_OUTPUT_TOKENS = int(os.getenv("AI_MAX_OUTPUT_TOKENS", "120"))
OLLAMA_TIMEOUT_MS = int(os.getenv("AI_OLLAMA_TIMEOUT_MS", "300000"))

# Cheap CPU-friendly knobs (reduce memory/latency).
OLLAMA_NUM_CTX = int(os.getenv("AI_OLLAMA_NUM_CTX", "1024"))
OLLAMA_NUM_BATCH = int(os.getenv("AI_OLLAMA_NUM_BATCH", "128"))
OLLAMA_NUM_THREAD = int(os.getenv("AI_OLLAMA_NUM_THREAD", "4"))
OLLAMA_KEEP_ALIVE = os.getenv("AI_OLLAMA_KEEP_ALIVE", "10m")


class ChatTurn(BaseModel):
    role: Literal["user", "assistant"]
    content: str


class ChatRequest(BaseModel):
    userId: str = Field(min_length=1)
    contextPrefix: str = Field(min_length=1)
    conversation: list[ChatTurn] = Field(default_factory=list)
    message: str = Field(min_length=1)


class ChatResponse(BaseModel):
    assistantText: str
    usage: dict | None = None
    # The gateway does not impersonate pets on failure. Callers should implement a per-pet fallback.
    fallbackUsed: bool = False


app = FastAPI(title="Local SLM Gateway", version="0.1.0")


@app.get("/health")
def health() -> dict:
    return {"ok": True, "model": OLLAMA_MODEL}

@app.get("/version")
def version() -> dict:
    return {"name": "local-slm-gateway", "version": "0.1.0"}


@app.get("/ready")
async def ready() -> dict:
    """
    Readiness check: verifies we can reach Ollama.
    Does not guarantee the model is already downloaded.
    """
    try:
        async with httpx.AsyncClient(timeout=2.5) as client:
            r = await client.get(f"{OLLAMA_BASE_URL}/api/tags")
            r.raise_for_status()
        return {"ok": True, "ollamaReachable": True, "model": OLLAMA_MODEL}
    except Exception:
        raise HTTPException(status_code=503, detail="Ollama not reachable")


def _truncate(s: str, max_chars: int) -> str:
    if len(s) <= max_chars:
        return s
    return s[: max_chars - 1] + "…"


def _build_system_prompt(context_prefix: str) -> str:
    # Caller provides the fixed prefix/context. We keep it short/cheap by enforcing max chars.
    return _truncate(context_prefix.strip(), MAX_PROMPT_CHARS)


@app.post("/v1/chat", response_model=ChatResponse)
async def chat(req: ChatRequest, request: Request, authorization: str | None = Header(default=None)) -> ChatResponse:
    # Debug-friendly: print request body size information.
    # (Safe: we only print sizes, not the API key and not the content.)
    try:
        body_bytes = await request.body()
        print(f"/v1/chat incoming: content-length={request.headers.get('content-length')} bodyBytes={len(body_bytes)}")
    except Exception:
        print("/v1/chat incoming: failed to read body for debug")

    if authorization != f"Bearer {API_KEY}":
        raise HTTPException(status_code=401, detail="Unauthorized")

    # Cost guardrails: keep context small and bounded.
    user_message = _truncate(req.message.strip(), MAX_PROMPT_CHARS)
    turns = req.conversation[-MAX_TURNS:]

    system = _build_system_prompt(req.contextPrefix)

    messages = [{"role": "system", "content": system}]
    for t in turns:
        messages.append({"role": t.role, "content": _truncate(t.content, MAX_PROMPT_CHARS)})
    messages.append({"role": "user", "content": user_message})

    started = time.time()
    try:
        async with httpx.AsyncClient(timeout=OLLAMA_TIMEOUT_MS / 1000.0) as client:
            r = await client.post(
                f"{OLLAMA_BASE_URL}/api/chat",
                json={
                    "model": OLLAMA_MODEL,
                    "messages": messages,
                    "stream": False,
                    "keep_alive": OLLAMA_KEEP_ALIVE,
                    "options": {
                        "num_predict": MAX_OUTPUT_TOKENS,
                        "num_ctx": OLLAMA_NUM_CTX,
                        "num_batch": OLLAMA_NUM_BATCH,
                        "num_thread": OLLAMA_NUM_THREAD,
                    },
                },
            )
            r.raise_for_status()
            data = r.json()
            content = (
                (data.get("message") or {}).get("content")
                if isinstance(data, dict)
                else None
            )
            if not isinstance(content, str) or not content.strip():
                raise RuntimeError("Empty model response")
            latency_ms = int((time.time() - started) * 1000)
            return ChatResponse(assistantText=content.strip(), usage={"latencyMs": latency_ms}, fallbackUsed=False)
    except Exception:
        latency_ms = int((time.time() - started) * 1000)
        raise HTTPException(status_code=503, detail=f"Model unavailable (latencyMs={latency_ms})")

