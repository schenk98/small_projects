# Comprehensive Roadmap: Město Staňkov Website Modernization

## Project Overview
**Goal:** Migrate `mestostankov.cz` from a legacy, document-heavy structure to a modern, lightweight, and highly accessible platform.
**Target Audience:** Citizens aged 50+, prioritizing clarity, high contrast, and intuitive search over flashy animations.
**Tech Stack:** Next.js (Frontend), Tailwind CSS (UI/Styling), Supabase or MongoDB (Database), Meilisearch (Search Engine).
**AI Tooling:** Cursor (Agentic IDE), Firecrawl / Crawl4AI (Scraping), v0.dev (UI Components).

---

## Phase 1: Content Audit & AI Scraping (Weeks 1-2)
*Objective: Extract, clean, and categorize existing data without manual copy-pasting.*

### Step 1.1: Web Scraping and Extraction
1. **Initialize Scraper:** Use `Crawl4AI` (Python library) or `Firecrawl` to target the old `mestostankov.cz` domain.
2. **Data Mapping:** Configure the scraper to bypass legacy HTML tables and extract pure content, converting it into structured Markdown or JSON.
3. **Document Harvesting:** Write a specific scraping rule to download or index all PDF links. Ensure you capture the metadata (Publish Date, Title, Category) primarily focusing on the *Úřední deska* (Official Board).

### Step 1.2: AI-Powered Information Architecture (IA)
1. **Categorization:** Feed the massive JSON/Markdown output into an LLM (via API or Cursor).
2. **Prompt Engineering:** *“Analyze these 500 pages/documents from a Czech town website. Reorganize them into 6 flat, intuitive categories designed for a 50-year-old user looking for municipal services. Output the new structure in a JSON hierarchy.”*
3. **Finalize Menu:** Settle on flat categories (e.g., *Samospráva, Život ve městě, Úřední deska, Formuláře, Kontakty*). Avoid deep nested dropdowns.

---

## Phase 2: System Architecture & Backend Setup (Week 3)
*Objective: Prepare the data infrastructure to handle heavy document loads.*

### Step 2.1: Database Provisioning (Supabase / MongoDB)
1. **Schema Design:** Create collections/tables for:
   - `pages` (Static content: history, contact info)
   - `news` (Announcements, cultural events)
   - `official_board` (Mandatory public notices with expiry dates)
   - `documents` (General forms, PDFs)
2. **Data Import:** Write a Node.js/Python script to bulk-insert the cleaned, categorized AI-scraped data into your new database.

### Step 2.2: Implementing the Search Engine (Meilisearch)
1. **Deployment:** Spin up a Meilisearch instance.
2. **Indexing:** Sync your database collections with Meilisearch.
3. **Tuning:** Configure typo tolerance and synonyms in Czech language (e.g., "odpad" = "popelnice" = "komunální"). This is critical for older users who might not use exact bureaucratic terms.

---

## Phase 3: Frontend Development with Cursor (Weeks 4-5)
*Objective: Build the user interface with an extreme focus on accessibility.*

### Step 3.1: Scaffolding the Next.js App
1. Open **Cursor**, press `Cmd/Ctrl + I` (Composer), and prompt: 
   *“Initialize a Next.js App Router project with Tailwind CSS and Shadcn UI. Set up a basic layout with a sticky header and a footer.”*
2. Configure environmental variables connecting Next.js to your Database and Meilisearch.

### Step 3.2: Designing for the 50+ Demographic
1. **Typography:** Set base font size to `18px` or `1.125rem`. Use a highly legible sans-serif font like Inter or Roboto.
2. **Color Palette:** Ensure WCAG AAA compliance. Dark charcoal text (`#1a1a1a`) on off-white/cream backgrounds (`#f8f9fa`). Use a distinct primary color (e.g., Staňkov's coat of arms blue) strictly for call-to-action buttons.
3. **The Search-First Paradigm:** Instead of forcing users to click through 4 layers of menus, place a massive, highly visible search bar in the center of the hero section.

### Step 3.3: Component Generation (v0.dev + Cursor)
1. **Hero Section:** Prompt v0 or Cursor to build a hero section featuring the search bar and 4-6 large, icon-based quick links (e.g., *Odpad, Poplatky, Úřední deska, Hlášení poruch*).
2. **Official Board (Úřední deska):** Build a data table component with clear dates, document links, and a filter for "Active vs. Expired" notices.
3. **Mobile Responsiveness:** Ensure buttons have a minimum touch target size of `44x44px`.

---

## Phase 4: Integration & Optimization (Weeks 6-7)
*Objective: Connect the frontend to the backend and ensure everything runs instantly.*

### Step 4.1: API Integration
1. Use Next.js Server Components to fetch static page data directly from the database, ensuring perfect SEO for Google.
2. Integrate the Meilisearch frontend client (`react-instantsearch-hooks-web`) into your global search bar for instant, keystroke-by-keystroke results.

### Step 4.2: CMS / Admin Panel
1. The town clerks need a way to upload new documents.
2. If using Supabase, you can leverage their auto-generated dashboards, or build a secure, simple Admin route in Next.js protected by NextAuth/Auth.js. 
3. *Recommendation:* Keep the admin panel dead simple. "Title", "Upload PDF", "Expiry Date".

---

## Phase 5: Testing, SEO, & Deployment (Week 8)
*Objective: Ensure a smooth transition from the old site to the new one.*

### Step 5.1: Accessibility and QA
1. Run Google Lighthouse. Aim for 100 on Accessibility.
2. Test the site entirely via keyboard navigation.
3. Test on older mobile devices, as your demographic may not have the latest iPhones.

### Step 5.2: The Redirect Map (Critical!)
1. Since the old site has been indexed by Google for years, you MUST map old URLs to the new ones.
2. Provide your AI scraper's URL list to Cursor and ask it to generate a `next.config.js` redirect map (e.g., `/stara-stranka-uradu.html` -> `/uredni-deska`).

### Step 5.3: Deployment
1. Deploy the Next.js application to **Vercel** for optimal performance and zero-configuration CI/CD.
2. Point the `mestostankov.cz` DNS records to Vercel.
3. Conduct a final live test.
