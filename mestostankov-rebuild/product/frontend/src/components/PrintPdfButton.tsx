"use client";

import { useMemo } from "react";

export function PrintPdfButton({ compact }: { compact?: boolean } = {}) {
  const label = useMemo(() => (compact ? "🖨️" : "Tisk / PDF"), [compact]);

  return (
    <button
      type="button"
      onClick={() => window.print()}
      className={compact ? "header-icon-btn" : "btn"}
      style={compact ? undefined : { padding: "0.5rem 0.75rem" }}
      title="Vytisknout stránku nebo uložit jako PDF"
    >
      {label}
    </button>
  );
}

