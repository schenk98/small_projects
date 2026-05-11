import Link from "next/link";

export type HubLink = {
  label: string;
  href: string;
  external?: boolean;
};

export function HubGrid({
  items,
}: {
  items: { title: string; href: string; description?: string }[];
}) {
  return (
    <div
      style={{
        display: "grid",
        gridTemplateColumns: "repeat(auto-fit, minmax(220px, 1fr))",
        gap: "0.75rem",
      }}
    >
      {items.map((it) => (
        <Link
          key={it.href}
          href={it.href}
          className="card card-hover"
          style={{
            padding: "1rem",
            textDecoration: "none",
            color: "inherit",
            display: "flex",
            flexDirection: "column",
            gap: "0.35rem",
            minHeight: "120px",
          }}
        >
          <div style={{ fontWeight: 900, fontSize: "1.1rem" }}>{it.title}</div>
          {it.description ? (
            <div style={{ color: "var(--muted)", fontSize: "0.95rem" }}>
              {it.description}
            </div>
          ) : null}
        </Link>
      ))}
    </div>
  );
}

export function LinkList({
  title,
  links,
}: {
  title: string;
  links: HubLink[];
}) {
  return (
    <div style={{ display: "flex", flexDirection: "column", gap: "0.5rem" }}>
      <div style={{ fontWeight: 900, color: "var(--muted)" }}>{title}</div>
      {links.map((l) =>
        l.external ? (
          <a
            key={`${l.href}::${l.label}`}
            href={l.href}
            target="_blank"
            rel="noreferrer"
            className="card card-hover"
            style={{
              padding: "0.85rem 1rem",
              textDecoration: "none",
              color: "var(--brand)",
              fontWeight: 800,
            }}
          >
            {l.label}
          </a>
        ) : (
          <Link
            key={`${l.href}::${l.label}`}
            href={l.href}
            className="card card-hover"
            style={{
              padding: "0.85rem 1rem",
              textDecoration: "none",
              color: "var(--brand)",
              fontWeight: 800,
            }}
          >
            {l.label}
          </Link>
        ),
      )}
    </div>
  );
}

