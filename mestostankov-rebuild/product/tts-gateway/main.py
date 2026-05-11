from __future__ import annotations

import asyncio
import os
from typing import Optional

from fastapi import FastAPI, HTTPException
from fastapi.responses import Response
from pydantic import BaseModel, Field


app = FastAPI(title="Staňkov TTS Gateway", version="0.1.0")

# The release tarball unpacks to /opt/piper/piper/..., and the executable is /opt/piper/piper/piper
PIPER_BIN = os.environ.get("PIPER_BIN", "/opt/piper/piper/piper")
DEFAULT_MODEL = os.environ.get("PIPER_MODEL", "/opt/voices/cs_CZ-jirka-low.onnx")
DEFAULT_CONFIG = os.environ.get("PIPER_CONFIG", "/opt/voices/cs_CZ-jirka-low.onnx.json")


class TtsReq(BaseModel):
    text: str = Field(min_length=1, max_length=8000)
    lang: str = "cs-CZ"
    voice: Optional[str] = None


async def synth_wav(text: str) -> bytes:
    proc = await asyncio.create_subprocess_exec(
        PIPER_BIN,
        "--model",
        DEFAULT_MODEL,
        "--config",
        DEFAULT_CONFIG,
        "--output_file",
        "-",
        stdin=asyncio.subprocess.PIPE,
        stdout=asyncio.subprocess.PIPE,
        stderr=asyncio.subprocess.PIPE,
    )
    # Note: When output_file is "-", piper writes wav bytes to stdout.
    stdout, stderr = await proc.communicate(input=text.encode("utf-8"))
    if proc.returncode != 0:
        raise RuntimeError(stderr.decode("utf-8", errors="replace")[:500])
    if not stdout:
        raise RuntimeError("No audio produced")
    return stdout


@app.get("/healthz")
def healthz():
    return {"ok": True}


@app.post("/tts")
async def tts(req: TtsReq):
    try:
        # We ignore lang/voice here for now; the gateway is pinned to Czech voice
        # to guarantee consistent, offline behavior.
        audio = await synth_wav(req.text)
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
    return Response(content=audio, media_type="audio/wav")

