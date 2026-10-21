// ==============================================================
// Finance System Manager - Centralized API Service & Helpers
// ==============================================================

const TOKEN_KEY = "fsm_token";
const USER_KEY = "fsm_user";

function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

function setSession(token, user) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

function getStoredUser() {
  const data = localStorage.getItem(USER_KEY);
  return data ? JSON.parse(data) : null;
}

function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

async function apiRequest(endpoint, method = "GET", body = null, isFormData = false) {
  const token = getToken();
  const headers = {};

  if (token) {
    headers["Authorization"] = `Bearer ${token}`;
  }

  if (!isFormData && body) {
    headers["Content-Type"] = "application/json";
  }

  const config = {
    method,
    headers,
  };

  if (body) {
    config.body = isFormData ? body : JSON.stringify(body);
  }

  try {
    const res = await fetch(`${API_BASE_URL}${endpoint}`, config);

    if (res.status === 401) {
      clearSession();
      showToast("Session expired or unauthorized. Please sign in.", "error");
      openAuthModal();
      throw new Error("Unauthorized");
    }

    if (res.status === 403) {
      const errData = await res.json().catch(() => ({}));
      const msg = errData.message || "Forbidden: You do not have permission for this operation.";
      showToast(msg, "error");
      throw new Error(msg);
    }

    if (!res.ok) {
      const errData = await res.json().catch(() => ({}));
      const msg = errData.message || `Request failed with status ${res.status}`;
      showToast(msg, "error");
      throw new Error(msg);
    }

    // Check if response is JSON or empty
    const contentType = res.headers.get("content-type");
    if (contentType && contentType.includes("application/json")) {
      return await res.json();
    } else if (contentType && (contentType.includes("text/csv") || contentType.includes("octet-stream"))) {
      return await res.blob();
    }
    return null;
  } catch (err) {
    console.error(`API Error [${method} ${endpoint}]:`, err);
    throw err;
  }
}

// Toast System
function showToast(message, type = "info") {
  const container = document.getElementById("toast-container");
  if (!container) return;

  const toast = document.createElement("div");
  toast.className = `toast toast-${type}`;
  toast.innerHTML = `<span>${message}</span>`;

  container.appendChild(toast);

  setTimeout(() => {
    toast.style.opacity = "0";
    toast.style.transform = "translateX(100%)";
    toast.style.transition = "all 0.3s ease";
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Modal Helpers
function openModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.add("active");
}

function closeModal(id) {
  const m = document.getElementById(id);
  if (m) m.classList.remove("active");
}

// Formatters
function formatCurrency(val) {
  if (val === undefined || val === null) return "₹0.00";
  const num = Number(val);
  return "₹" + num.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function formatDate(dateStr) {
  if (!dateStr) return "-";
  try {
    const d = new Date(dateStr);
    return d.toLocaleDateString("en-IN", { year: "numeric", month: "short", day: "numeric" });
  } catch {
    return dateStr;
  }
}

// Global modal background click handler
window.addEventListener("click", (e) => {
  if (e.target.classList.contains("modal-overlay") && e.target.id !== "auth-modal") {
    e.target.classList.remove("active");
  }
});
