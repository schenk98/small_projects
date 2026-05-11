"use client";

import { useMemo, useRef, useState, useSyncExternalStore } from "react";

function getSpeakableText(): string {
  const el =
    document.querySelector(".content-prose") ??
    document.querySelector("main");
  const txt = el?.textContent ?? "";
  return txt.replace(/\s+/g, " ").trim();
}

export function TtsButton() {
  const supported = useSyncExternalStore(
    () => () => {},
    () => typeof window !== "undefined" && "speechSynthesis" in window,
    () => false,
  );
  const [speaking, setSpeaking] = useState(false);
  const [serverError, setServerError] = useState<string | null>(null);
  const audioRef = useRef<HTMLAudioElement | null>(null);
  const abortRef = useRef<AbortController | null>(null);

  const label = useMemo(() => {
    if (!supported) return "Předčítání není dostupné";
    return speaking ? "Zastavit čtení" : "Předčítat";
  }, [speaking, supported]);

  async function speakViaServer(text: string): Promise<boolean> {
    try {
      abortRef.current?.abort();
      const ac = new AbortController();
      abortRef.current = ac;
      setServerError(null);

      const res = await fetch("/api/tts", {
        method: "POST",
        headers: { "content-type": "application/json" },
        body: JSON.stringify({ text, lang: "cs-CZ" }),
        signal: ac.signal,
      });

      if (!res.ok) {
        const msg = await res.text().catch(() => "");
        setServerError(msg.slice(0, 300) || `TTS server error (${res.status})`);
        return false;
      }
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);

      if (!audioRef.current) audioRef.current = new Audio();
      const a = audioRef.current;
      a.src = url;
      a.onended = () => setSpeaking(false);
      a.onerror = () => setSpeaking(false);
      setSpeaking(true);
      await a.play();
      return true;
    } catch {
      setServerError("TTS server is unreachable.");
      return false;
    }
  }

  async function toggle() {
    if (!supported) return;
    if (speaking) {
      abortRef.current?.abort();
      audioRef.current?.pause();
      if (audioRef.current) audioRef.current.currentTime = 0;
      window.speechSynthesis.cancel();
      setSpeaking(false);
      return;
    }
    const text = getSpeakableText();
    if (!text) return;

    const ok = await speakViaServer(text);
    if (!ok) setSpeaking(false);
  }

  return (
    <div style={{ display: "flex", gap: "0.5rem", alignItems: "center", flexWrap: "wrap" }}>
      <button
        type="button"
        onClick={toggle}
        className={speaking ? "btn" : "header-icon-btn"}
        style={speaking ? { padding: "0.5rem 0.75rem" } : undefined}
        aria-label={label}
        title={label}
      >
        {speaking ? `🔊 ${label}` : "🔊"}
      </button>
      {serverError ? (
        <span style={{ fontSize: "0.85rem", color: "var(--muted)", fontWeight: 700 }}>
          (Předčítání je dočasně nedostupné.)
        </span>
      ) : null}
    </div>
  );
}

