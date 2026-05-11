"use client";

import { useEffect, useSyncExternalStore } from "react";

type Theme = "system" | "light" | "dark";

const THEME_EVENT = "stankov-theme";

function getStoredTheme(): Theme {
  const v = typeof window !== "undefined" ? window.localStorage.getItem("theme") : null;
  if (v === "light" || v === "dark" || v === "system") return v;
  return "system";
}

function applyTheme(t: Theme) {
  const el = document.documentElement;
  el.removeAttribute("data-theme");
  if (t === "light") el.setAttribute("data-theme", "light");
  if (t === "dark") el.setAttribute("data-theme", "dark");
}

function subscribe(onStoreChange: () => void) {
  if (typeof window === "undefined") return () => {};
  const handler = () => onStoreChange();
  window.addEventListener("storage", handler);
  window.addEventListener(THEME_EVENT, handler);
  return () => {
    window.removeEventListener("storage", handler);
    window.removeEventListener(THEME_EVENT, handler);
  };
}

function getThemeSnapshot(): Theme {
  return getStoredTheme();
}

function getServerThemeSnapshot(): Theme {
  return "system";
}

export function ThemeToggle() {
  const theme = useSyncExternalStore(subscribe, getThemeSnapshot, getServerThemeSnapshot);

  useEffect(() => {
    applyTheme(theme);
  }, [theme]);

  function cycle() {
    const next: Theme = theme === "system" ? "light" : theme === "light" ? "dark" : "system";
    window.localStorage.setItem("theme", next);
    applyTheme(next);
    window.dispatchEvent(new Event(THEME_EVENT));
  }

  const icon = theme === "dark" ? "🌙" : theme === "light" ? "☀️" : "🌓";
  const label = theme === "system" ? "Auto" : theme === "light" ? "Světlý" : "Tmavý";

  return (
    <button
      type="button"
      onClick={cycle}
      className="header-icon-btn"
      aria-label={`Motiv: ${label}`}
      title={`Motiv: ${label}`}
    >
      {icon}
    </button>
  );
}
