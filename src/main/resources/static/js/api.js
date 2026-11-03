// ===== Shared helpers used by every page =====

// Sends a request to our Spring Boot backend and returns the JSON response.
// The login token is attached so the server knows who we are.
async function api(method, url, body) {
    const options = { method, headers: { "Content-Type": "application/json" } };
    const token = localStorage.getItem("token");
    if (token) options.headers["Authorization"] = "Bearer " + token;
    if (body) options.body = JSON.stringify(body);

    const response = await fetch(url, options);
    const data = await response.json().catch(() => ({}));

    if (response.status === 401 && token) {          // token expired / server restarted
        localStorage.clear();
        window.location.href = "/index.html";
        throw new Error("Session expired. Please login again.");
    }
    if (!response.ok) throw new Error(data.message || "Request failed");
    return data;
}

// 12345.5 -> "₹12,345.50" (Indian number format)
function money(value) {
    return "\u20B9" + Number(value || 0).toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(value) {
    if (!value) return "";
    return new Date(value).toLocaleString("en-IN", { day: "2-digit", month: "short", year: "numeric", hour: "2-digit", minute: "2-digit" });
}

function formatDay(value) {
    return new Date(value).toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" });
}

function maskAccount(number) {
    return number ? "XXXXXXX" + number.slice(-4) : "";
}

// Small popup message at the bottom of the screen
function toast(message, type = "") {
    let el = document.getElementById("toast");
    if (!el) {
        el = document.createElement("div");
        el.id = "toast";
        document.body.appendChild(el);
    }
    el.textContent = message;
    el.className = "show " + type;
    clearTimeout(el.timer);
    el.timer = setTimeout(() => (el.className = ""), 3000);
}

// Escapes text before putting it inside HTML (prevents XSS)
function esc(text) {
    const div = document.createElement("div");
    div.textContent = text == null ? "" : String(text);
    return div.innerHTML;
}

async function logout() {
    try { await api("POST", "/api/auth/logout"); } catch (e) { /* ignore */ }
    localStorage.clear();
    window.location.href = "/index.html";
}

function badge(status) {
    const colors = { ACTIVE: "green", APPROVED: "green", PENDING: "yellow", FROZEN: "red", REJECTED: "red", CLOSED: "grey", BLOCKED: "red" };
    return `<span class="badge badge-${colors[status] || "grey"}">${esc(status)}</span>`;
}
