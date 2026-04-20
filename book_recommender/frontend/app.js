const prepareButton = document.getElementById("prepare-btn");
const prepareStatus = document.getElementById("prepare-status");
const settingsToggleButton = document.getElementById("settings-toggle");
const settingsDropdown = document.getElementById("settings-dropdown");
const recommendForm = document.getElementById("recommend-form");
const recommendStatus = document.getElementById("recommend-status");
const targetTitleInput = document.getElementById("target-title");
const targetIsbnInput = document.getElementById("target-isbn");
const titleSuggestions = document.getElementById("title-suggestions");
const titleSuggestionsBody = titleSuggestions.querySelector("tbody");
const authorSubstringInput = document.getElementById("author-substring");
const thresholdInput = document.getElementById("threshold");
const topNInput = document.getElementById("top-n");
const tableBody = document.querySelector("#results-table tbody");
const suggestionIndex = new Map();
targetTitleInput.value = "the fellowship of the ring (the lord of the rings, part 1)";
authorSubstringInput.value = "";
prepareButton.addEventListener("click", async () => {
    prepareButton.disabled = true;
    prepareStatus.textContent = "Preparing data... this may take a while.";
    try {
        const response = await fetch("/api/prepare-data", { method: "POST" });
        const payload = await response.json();
        if (!response.ok || !payload.ok) {
            throw new Error(payload.message || "Data preparation failed");
        }
        prepareStatus.textContent = payload.message;
    }
    catch (error) {
        prepareStatus.textContent = `Error: ${error.message}`;
    }
    finally {
        prepareButton.disabled = false;
    }
});
settingsToggleButton.addEventListener("click", () => {
    settingsDropdown.classList.toggle("visible");
});
document.addEventListener("click", (event) => {
    const clickTarget = event.target;
    if (!(clickTarget instanceof HTMLElement)) {
        return;
    }
    if (clickTarget.closest(".settings-menu")) {
        return;
    }
    settingsDropdown.classList.remove("visible");
});
recommendForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    recommendStatus.textContent = "Fetching recommendations...";
    if (tableBody) {
        tableBody.innerHTML = "";
    }
    const resolvedInput = targetTitleInput.value.trim();
    const isbnInput = targetIsbnInput.value.trim();
    if (!resolvedInput && !isbnInput) {
        recommendStatus.textContent = "Please provide a title or an ISBN.";
        return;
    }
    const body = {
        target_title: targetTitleInput.value,
        target_isbn: isbnInput || null,
        target_author_substring: authorSubstringInput.value,
        rating_threshold: Number(thresholdInput.value),
        top_n: Number(topNInput.value),
    };
    try {
        const response = await fetch("/api/recommend", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(body),
        });
        const payload = await response.json();
        if (!response.ok || !payload.ok) {
            throw new Error(payload.detail || payload.message || "Recommendation request failed");
        }
        const items = payload.items;
        renderRows(items);
        const matchedTitle = payload.matched_title;
        const matchedIsbn = payload.matched_isbn;
        const returnedCount = Number(payload.returned_count ?? items.length);
        const totalCandidates = Number(payload.total_candidates ?? items.length);
        targetTitleInput.value = matchedTitle || targetTitleInput.value;
        if (matchedIsbn) {
            targetIsbnInput.value = matchedIsbn;
        }
        recommendStatus.textContent =
            `Done. Showing ${returnedCount} of ${totalCandidates} available recommendations. ` +
                `Matched title: "${matchedTitle}" (ISBN: ${matchedIsbn}).`;
    }
    catch (error) {
        recommendStatus.textContent = `Error: ${error.message}`;
    }
});
let suggestionTimer;
targetTitleInput.addEventListener("input", () => {
    // Prefer ISBN as source of truth when title maps to known suggestion.
    syncIsbnFromTypedTitle();
    // Debounce requests so we do not call suggestions on every keystroke.
    if (suggestionTimer) {
        window.clearTimeout(suggestionTimer);
    }
    suggestionTimer = window.setTimeout(() => {
        fetchTitleSuggestions(targetTitleInput.value).catch((error) => {
            console.error("Failed to load title suggestions", error);
        });
    }, 250);
});
titleSuggestionsBody.addEventListener("click", (event) => {
    const clickTarget = event.target;
    if (!(clickTarget instanceof HTMLElement)) {
        return;
    }
    const selectedRow = clickTarget.closest("tr[data-title]");
    if (!selectedRow) {
        return;
    }
    applySuggestionSelection(selectedRow.dataset.title ?? "", selectedRow.dataset.isbn ?? "", selectedRow.dataset.author ?? "");
});
function renderRows(items) {
    if (!tableBody)
        return;
    // Render one row per recommendation with backend-computed metrics.
    for (const item of items) {
        const corrValue = Number(item.corr);
        const avgRatingValue = Number(item.avg_rating);
        const row = document.createElement("tr");
        row.innerHTML = `
      <td>${escapeHtml(item.book)}</td>
      <td>${Number.isFinite(corrValue) ? corrValue.toFixed(6) : "n/a"}</td>
      <td>${item.rating_count}</td>
      <td>${Number.isFinite(avgRatingValue) ? avgRatingValue.toFixed(3) : "n/a"}</td>
    `;
        tableBody.appendChild(row);
    }
}
async function fetchTitleSuggestions(query) {
    const trimmed = query.trim();
    // Keep autocomplete quiet until user provides meaningful input.
    if (trimmed.length < 2) {
        clearSuggestions();
        return;
    }
    const params = new URLSearchParams({ q: trimmed, top_n: "8" });
    const response = await fetch(`/api/title-suggestions?${params.toString()}`);
    const payload = await response.json();
    if (!response.ok || !payload.ok) {
        clearSuggestions();
        return;
    }
    renderTitleSuggestions(payload.items);
}
function renderTitleSuggestions(items) {
    clearSuggestions();
    suggestionIndex.clear();
    // Render helper table with clickable rows.
    if (items.length === 0) {
        const emptyRow = document.createElement("tr");
        emptyRow.innerHTML = `<td class="suggestion-empty" colspan="5">No suggestions found.</td>`;
        titleSuggestionsBody.appendChild(emptyRow);
        return;
    }
    for (const item of items) {
        const option = document.createElement("tr");
        option.dataset.title = item.title;
        option.dataset.isbn = item.isbn;
        option.dataset.author = item.author;
        option.innerHTML = `
      <td>${escapeHtml(item.title)}</td>
      <td>${escapeHtml(item.author)}</td>
      <td>${item.rating_count}</td>
      <td>${item.avg_rating.toFixed(3)}</td>
      <td>${escapeHtml(item.isbn)}</td>
    `;
        suggestionIndex.set(item.title.toLowerCase(), { isbn: item.isbn, author: item.author });
        titleSuggestionsBody.appendChild(option);
    }
}
function clearSuggestions() {
    titleSuggestionsBody.innerHTML = "";
}
function syncIsbnFromTypedTitle() {
    const selected = suggestionIndex.get(targetTitleInput.value.trim().toLowerCase());
    if (selected?.isbn) {
        targetIsbnInput.value = selected.isbn;
    }
    if (selected?.author) {
        authorSubstringInput.value = selected.author;
    }
}
function applySuggestionSelection(title, isbn, author) {
    targetTitleInput.value = title;
    targetIsbnInput.value = isbn;
    if (author) {
        authorSubstringInput.value = author;
        return;
    }
    const selected = suggestionIndex.get(title.trim().toLowerCase());
    if (selected?.author) {
        authorSubstringInput.value = selected.author;
    }
}
function escapeHtml(input) {
    // Escape injected text before writing HTML to avoid rendering issues.
    return input
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
