const prepareButton = document.getElementById("prepare-btn");
const prepareStatus = document.getElementById("prepare-status");
const recommendForm = document.getElementById("recommend-form");
const recommendStatus = document.getElementById("recommend-status");
const targetTitleInput = document.getElementById("target-title");
const titleSuggestions = document.getElementById("title-suggestions");
const authorSubstringInput = document.getElementById("author-substring");
const thresholdInput = document.getElementById("threshold");
const topNInput = document.getElementById("top-n");
const tableBody = document.querySelector("#results-table tbody");
targetTitleInput.value = "the fellowship of the ring (the lord of the rings, part 1)";
authorSubstringInput.value = "tolkien";
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
recommendForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    recommendStatus.textContent = "Fetching recommendations...";
    if (tableBody) {
        tableBody.innerHTML = "";
    }
    const body = {
        target_title: targetTitleInput.value,
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
        recommendStatus.textContent = `Done. Received ${items.length} recommendations. Matched title: "${matchedTitle}".`;
    }
    catch (error) {
        recommendStatus.textContent = `Error: ${error.message}`;
    }
});
let suggestionTimer;
targetTitleInput.addEventListener("input", () => {
    if (suggestionTimer) {
        window.clearTimeout(suggestionTimer);
    }
    suggestionTimer = window.setTimeout(() => {
        fetchTitleSuggestions(targetTitleInput.value).catch((error) => {
            console.error("Failed to load title suggestions", error);
        });
    }, 250);
});
function renderRows(items) {
    if (!tableBody)
        return;
    for (const item of items) {
        const row = document.createElement("tr");
        row.innerHTML = `
      <td>${escapeHtml(item.book)}</td>
      <td>${item.corr.toFixed(6)}</td>
      <td>${item.rating_count}</td>
      <td>${item.avg_rating.toFixed(3)}</td>
    `;
        tableBody.appendChild(row);
    }
}
async function fetchTitleSuggestions(query) {
    const trimmed = query.trim();
    if (trimmed.length < 2) {
        titleSuggestions.innerHTML = "";
        return;
    }
    const params = new URLSearchParams({ q: trimmed, top_n: "8" });
    const response = await fetch(`/api/title-suggestions?${params.toString()}`);
    const payload = await response.json();
    if (!response.ok || !payload.ok) {
        return;
    }
    renderTitleSuggestions(payload.items);
}
function renderTitleSuggestions(items) {
    titleSuggestions.innerHTML = "";
    for (const item of items) {
        const option = document.createElement("option");
        option.value = item.title;
        option.label = `${item.title} (${item.author}, ratings: ${item.rating_count})`;
        titleSuggestions.appendChild(option);
    }
}
function escapeHtml(input) {
    return input
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}
