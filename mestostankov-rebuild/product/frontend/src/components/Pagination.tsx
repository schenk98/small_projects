import Link from "next/link";

export function Pagination({
  baseHref,
  page,
  totalPages,
}: {
  baseHref: string; // path without query, e.g. "/informace-pro-obcany/aktuality/"
  page: number; // 1-based
  totalPages: number;
}) {
  if (totalPages <= 1) return null;

  const mk = (p: number) => (p === 1 ? baseHref : `${baseHref}?page=${p}`);

  // Simple windowed pagination
  const window = 2;
  const pages: number[] = [];
  const start = Math.max(1, page - window);
  const end = Math.min(totalPages, page + window);
  for (let p = start; p <= end; p++) pages.push(p);

  return (
    <nav
      aria-label="Pagination"
      style={{
        display: "flex",
        gap: "0.5rem",
        alignItems: "center",
        flexWrap: "wrap",
        marginTop: "1rem",
      }}
    >
      <Link
        href={mk(Math.max(1, page - 1))}
        aria-disabled={page <= 1}
        className="card"
        style={{
          padding: "0.5rem 0.75rem",
          textDecoration: "none",
          color: "inherit",
          opacity: page <= 1 ? 0.5 : 1,
          pointerEvents: page <= 1 ? "none" : "auto",
        }}
      >
        ← Předchozí
      </Link>

      {start > 1 ? (
        <>
          <Link href={mk(1)} className="card" style={{ padding: "0.5rem 0.75rem", textDecoration: "none" }}>
            1
          </Link>
          {start > 2 ? <span style={{ color: "var(--muted)" }}>…</span> : null}
        </>
      ) : null}

      {pages.map((p) => (
        <Link
          key={p}
          href={mk(p)}
          aria-current={p === page ? "page" : undefined}
          className="card"
          style={{
            padding: "0.5rem 0.75rem",
            textDecoration: "none",
            fontWeight: p === page ? 900 : 700,
            color: p === page ? "white" : "inherit",
            background: p === page ? "var(--brand)" : "var(--surface)",
            borderColor: "var(--border)",
          }}
        >
          {p}
        </Link>
      ))}

      {end < totalPages ? (
        <>
          {end < totalPages - 1 ? <span style={{ color: "var(--muted)" }}>…</span> : null}
          <Link
            href={mk(totalPages)}
            className="card"
            style={{ padding: "0.5rem 0.75rem", textDecoration: "none" }}
          >
            {totalPages}
          </Link>
        </>
      ) : null}

      <Link
        href={mk(Math.min(totalPages, page + 1))}
        aria-disabled={page >= totalPages}
        className="card"
        style={{
          padding: "0.5rem 0.75rem",
          textDecoration: "none",
          color: "inherit",
          opacity: page >= totalPages ? 0.5 : 1,
          pointerEvents: page >= totalPages ? "none" : "auto",
        }}
      >
        Další →
      </Link>
    </nav>
  );
}

