import type { Metadata } from "next";

import "./globals.css";

export const metadata: Metadata = {
  title: {
    template: "%s | Město Staňkov",
    default: "Město Staňkov",
  },
  description: "Modernizované oficiální stránky města Staňkov.",
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="cs" className="h-full">
      <body className="min-h-full flex flex-col">{children}</body>
    </html>
  );
}
