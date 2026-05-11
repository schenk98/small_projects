FROM python:3.12-slim

WORKDIR /app

ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1

RUN apt-get update && apt-get install -y --no-install-recommends \
    ca-certificates curl tar \
  && rm -rf /var/lib/apt/lists/*

# Install Piper binary
RUN mkdir -p /opt/piper \
  && curl -fsSL "https://github.com/rhasspy/piper/releases/download/2023.11.14-2/piper_linux_x86_64.tar.gz" -o /tmp/piper.tar.gz \
  && tar -xzf /tmp/piper.tar.gz -C /opt/piper \
  && rm /tmp/piper.tar.gz \
  && chmod +x /opt/piper/piper/piper \
  && ln -sf /opt/piper/piper/piper /usr/local/bin/piper

# Download Czech voice model (Jirka - low quality, fast)
RUN mkdir -p /opt/voices \
  && curl -fsSL "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/cs/cs_CZ/jirka/low/cs_CZ-jirka-low.onnx" -o /opt/voices/cs_CZ-jirka-low.onnx \
  && curl -fsSL "https://huggingface.co/rhasspy/piper-voices/resolve/v1.0.0/cs/cs_CZ/jirka/low/cs_CZ-jirka-low.onnx.json" -o /opt/voices/cs_CZ-jirka-low.onnx.json

COPY requirements.txt /app/requirements.txt
RUN pip install --no-cache-dir -r /app/requirements.txt

COPY main.py /app/main.py

EXPOSE 8087
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8087"]

