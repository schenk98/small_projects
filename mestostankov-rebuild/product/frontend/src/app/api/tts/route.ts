import { NextResponse } from "next/server";

export const runtime = "nodejs";

type TtsRequest = {
  text: string;
  lang?: string; // BCP-47, e.g. "cs-CZ"
  voice?: string; // optional explicit voice id (gateway dependent)
};

function clampText(text: string): string {
  const t = (text ?? "").toString().trim();
  // hard safety limits: prevent abuse + huge payloads
  return t.length > 8000 ? t.slice(0, 8000) : t;
}

export async function POST(req: Request) {
  const gateway = process.env.TTS_GATEWAY_URL;
  if (!gateway) {
    return NextResponse.json(
      { error: "TTS gateway not configured. Set TTS_GATEWAY_URL." },
      { status: 501 },
    );
  }

  let body: TtsRequest;
  try {
    body = (await req.json()) as TtsRequest;
  } catch {
    return NextResponse.json({ error: "Invalid JSON" }, { status: 400 });
  }

  const text = clampText(body.text);
  if (!text) return NextResponse.json({ error: "Missing text" }, { status: 400 });

  const lang = (body.lang ?? "cs-CZ").toString();
  const voice = body.voice ? body.voice.toString() : undefined;

  const upstream = await fetch(new URL("/tts", gateway), {
    method: "POST",
    headers: { "content-type": "application/json" },
    body: JSON.stringify({ text, lang, voice }),
  });

  if (!upstream.ok) {
    const msg = await upstream.text().catch(() => "");
    return NextResponse.json(
      { error: `Upstream TTS error (${upstream.status})`, details: msg.slice(0, 500) },
      { status: 502 },
    );
  }

  const audio = await upstream.arrayBuffer();
  const contentType = upstream.headers.get("content-type") || "audio/mpeg";

  return new NextResponse(audio, {
    status: 200,
    headers: {
      "content-type": contentType,
      // cache per text is hard; keep short-lived to reduce repeat hits
      "cache-control": "private, max-age=300",
    },
  });
}

