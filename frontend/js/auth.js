// ==============================================================
// Authentication & Role-Based Session Management
// ==============================================================

let currentUser = null;

function switchAuthTab(tab) {
  const loginForm = document.getElementById("login-form");
  const registerForm = document.getElementById("register-form");
  const tabLogin = document.getElementById("tab-login");
  const tabRegister = document.getElementById("tab-register");
  const title = document.getElementById("auth-modal-title");

  if (tab === "login") {
    loginForm.style.display = "block";
    registerForm.style.display = "none";
    tabLogin.classList.add("active");
    tabRegister.classList.remove("active");
    title.textContent = "Sign In to Finance System Manager";
  } else {
    loginForm.style.display = "none";
    registerForm.style.display = "block";
    tabLogin.classList.remove("active");
    tabRegister.classList.add("active");
    title.textContent = "Register Account";
  }
}

function quickFillDemo(role) {
  switchAuthTab("login");
  const emailInput = document.getElementById("login-email");
  const passwordInput = document.getElementById("login-password");

  if (role === "student") {
    emailInput.value = "student@university.edu";
    passwordInput.value = "Password123!";
  } else {
    emailInput.value = "parent@family.com";
    passwordInput.value = "Password123!";
  }
}

async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value;

  try {
    const res = await apiRequest("/auth/login", "POST", { email, password });
    setSession(res.token, {
      userId: res.userId,
      email: res.email,
      displayName: res.displayName,
      role: res.role,
      invitationCode: res.invitationCode
    });

    closeModal("auth-modal");
    showToast(`Welcome back, ${res.displayName}!`, "success");
    await initializeApp();
  } catch (err) {
    // Handled in api.js
  }
}

async function handleRegisterSubmit(e) {
  e.preventDefault();
  const role = document.getElementById("register-role").value;
  const displayName = document.getElementById("register-name").value.trim();
  const email = document.getElementById("register-email").value.trim();
  const password = document.getElementById("register-password").value;

  try {
    const res = await apiRequest("/auth/register", "POST", { email, password, displayName, role });
    setSession(res.token, {
      userId: res.userId,
      email: res.email,
      displayName: res.displayName,
      role: res.role,
      invitationCode: res.invitationCode
    });

    closeModal("auth-modal");
    showToast(`Account created successfully! Welcome, ${res.displayName}.`, "success");
    await initializeApp();
  } catch (err) {
    // Handled in api.js
  }
}

function openAuthModal() {
  openModal("auth-modal");
}

function applyRoleRestrictions(role) {
  const isParent = role === "PARENT";
  const parentBanner = document.getElementById("parent-read-only-banner");
  const roleBadge = document.getElementById("header-role-badge");

  if (isParent) {
    parentBanner.style.display = "flex";
    roleBadge.textContent = "PARENT MODE (READ-ONLY)";
    roleBadge.className = "role-badge parent";

    // Disable or hide mutation buttons
    document.querySelectorAll(".student-only").forEach(el => {
      el.style.display = "none";
    });
  } else {
    parentBanner.style.display = "none";
    roleBadge.textContent = "STUDENT MODE";
    roleBadge.className = "role-badge student";

    // Show student controls
    document.querySelectorAll(".student-only").forEach(el => {
      el.style.display = "";
    });
  }
}

async function initializeApp() {
  const stored = getStoredUser();
  if (!stored || !getToken()) {
    openAuthModal();
    return;
  }

  currentUser = stored;
  document.getElementById("user-display-name").textContent = currentUser.displayName;
  document.getElementById("user-email").textContent = currentUser.email;

  applyRoleRestrictions(currentUser.role);

  // Load Categories & Current View
  await loadCategories();
  await loadDashboard();
}

// Navigation Handling
function navigateTo(viewId) {
  document.querySelectorAll(".app-view").forEach(v => v.classList.remove("active-view"));
  document.querySelectorAll(".nav-item").forEach(n => n.classList.remove("active"));

  const targetView = document.getElementById(viewId);
  const targetNav = document.querySelector(`.nav-item[data-view="${viewId}"]`);

  if (targetView) targetView.classList.add("active-view");
  if (targetNav) targetNav.classList.add("active");

  const titles = {
    "dashboard-view": "Personal Finance Dashboard",
    "transactions-view": "Transaction Ledger",
    "budgets-view": "Budget Management",
    "goals-view": "Savings Goals",
    "investments-view": "Investment Portfolio",
    "sip-view": "Systematic Investment Plans",
    "family-view": "Family Linking & Permissions",
    "statement-view": "Smart Statement Import",
    "intelligence-view": "Nine Intelligent Features Suite"
  };

  document.getElementById("page-title").textContent = titles[viewId] || "Finance System Manager";

  // Close mobile sidebar drawer if open
  const sidebar = document.querySelector(".sidebar");
  const overlay = document.getElementById("sidebar-overlay");
  if (sidebar && overlay) {
    sidebar.classList.remove("mobile-open");
    overlay.classList.remove("active");
  }

  // Trigger relevant view load
  if (viewId === "dashboard-view") loadDashboard();
  else if (viewId === "transactions-view") loadTransactions();
  else if (viewId === "budgets-view") loadBudgets();
  else if (viewId === "goals-view") loadGoals();
  else if (viewId === "investments-view") loadInvestments();
  else if (viewId === "sip-view") loadSips();
  else if (viewId === "family-view") loadFamilyLinks();
  else if (viewId === "intelligence-view") loadIntelligenceSuite();
}

// Mobile sidebar navigation drawer setup
function setupMobileNav() {
  const toggleBtn = document.getElementById("mobile-menu-toggle");
  const overlay = document.getElementById("sidebar-overlay");
  const sidebar = document.querySelector(".sidebar");

  if (toggleBtn && sidebar && overlay) {
    toggleBtn.addEventListener("click", () => {
      sidebar.classList.toggle("mobile-open");
      overlay.classList.toggle("active");
    });

    overlay.addEventListener("click", () => {
      sidebar.classList.remove("mobile-open");
      overlay.classList.remove("active");
    });
  }
}

// Attach Nav click handlers
document.querySelectorAll(".nav-item").forEach(btn => {
  btn.addEventListener("click", () => {
    const view = btn.getAttribute("data-view");
    if (view) navigateTo(view);
  });
});

// Logout
document.getElementById("btn-logout").addEventListener("click", () => {
  clearSession();
  currentUser = null;
  showToast("Logged out successfully.", "info");
  openAuthModal();
});

// CSV Export
document.getElementById("btn-export-csv").addEventListener("click", async () => {
  try {
    const blob = await apiRequest("/reports/export-csv");
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = "finance_transactions.csv";
    document.body.appendChild(a);
    a.click();
    a.remove();
    showToast("Transactions CSV downloaded.", "success");
  } catch (err) {
    // Handled in api.js
  }
});

// Boot application on DOM ready
document.addEventListener("DOMContentLoaded", () => {
  setupMobileNav();
  initializeApp();
});
