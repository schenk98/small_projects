const fs = require("node:fs");
const path = require("node:path");
const cheerio = require("cheerio");

const htmlPath = path.join(
  __dirname,
  "..", // frontend
  "..", // product
  "..", // mestostankov-rebuild
  "support",
  "scraper",
  "out",
  "20260508-202940",
  "raw",
  "pages",
  "436c2fe4cde1c09249637ddf.html",
);

const html = fs.readFileSync(htmlPath, "utf8");
const $ = cheerio.load(html);

const root = $(".editor_content.readable").first();
console.log("found", root.length);
console.log(root.text().trim().slice(0, 800));

