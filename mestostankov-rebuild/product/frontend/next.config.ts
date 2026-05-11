import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  images: {
    remotePatterns: [
      {
        protocol: "https",
        hostname: "www.mestostankov.cz",
        pathname: "/**",
      },
    ],
  },
  experimental: {
    // We read local dataset files from the server runtime.
    // This ensures Next doesn't try to bundle node:fs into the client.
    serverActions: {},
  },
};

export default nextConfig;
