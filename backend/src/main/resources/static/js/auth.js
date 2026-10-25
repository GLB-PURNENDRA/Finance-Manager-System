// ==============================================================
// Authentication & Role-Based Session Management
// ==============================================================

let currentUser = null;
let currentLoginRole = "STUDENT";
let currentRegisterRole = "STUDENT";

// Switch between Main Modes: 'login', 'register', 'forgot'
function switchAuthMode(mode) {
  const viewLogin = document.getElementById("auth-view-login");
  const viewRegister = document.getElementById("auth-view-register");
  const viewForgot = document.getElementById("auth-view-forgot");
  const mainTabs = document.getElementById("auth-main-tabs");
  const tabLogin = document.getElementById("tab-login");
  const tabRegister = document.getElementById("tab-register");
  const title = document.getElementById("auth-modal-title");
  const subtitle = document.getElementById("auth-modal-subtitle");

  if (mode === "login") {
    viewLogin.style.display = "block";
    viewRegister.style.display = "none";
    viewForgot.style.display = "none";
    mainTabs.style.display = "flex";
    tabLogin.classList.add("active");
    tabRegister.classList.remove("active");
    title.textContent = "Sign In to Finance Manager";
    subtitle.textContent = "Select your portal to securely access your family financial workspace.";
    updateLoginPortalUI();
  } else if (mode === "register") {
    viewLogin.style.display = "none";
    viewRegister.style.display = "block";
    viewForgot.style.display = "none";
    mainTabs.style.display = "flex";
    tabLogin.classList.remove("active");
    tabRegister.classList.add("active");
    title.textContent = "Register Real Account";
    subtitle.textContent = "Create your permanent, encrypted account. Passwords are saved with BCrypt hashing.";
    updateRegisterPortalUI();
  } else if (mode === "forgot") {
    viewLogin.style.display = "none";
    viewRegister.style.display = "none";
    viewForgot.style.display = "block";
    mainTabs.style.display = "none";
    title.textContent = "Reset Account Password";
    subtitle.textContent = "Generate a secure verification code to recover your account credentials.";
    document.getElementById("forgot-step-1").style.display = "block";
    document.getElementById("forgot-step-2").style.display = "none";
    document.getElementById("reset-code-display-box").style.display = "none";
  }
}

// Role selection in Login view
function selectLoginRole(role) {
  currentLoginRole = role;
  updateLoginPortalUI();
}

function updateLoginPortalUI() {
  const studentBtn = document.getElementById("portal-role-student");
  const parentBtn = document.getElementById("portal-role-parent");
  const indicatorText = document.getElementById("active-portal-text");
  const submitText = document.getElementById("login-submit-text");

  if (!studentBtn || !parentBtn) return;

  if (currentLoginRole === "STUDENT") {
    studentBtn.classList.add("active");
    parentBtn.classList.remove("active");
    if (indicatorText) {
      indicatorText.innerHTML = `Active Portal: <strong style="color: var(--primary-bright);">🎓 Student Login Portal</strong>`;
    }
    if (submitText) {
      submitText.textContent = "Sign In as Student";
    }
  } else {
    parentBtn.classList.add("active");
    studentBtn.classList.remove("active");
    if (indicatorText) {
      indicatorText.innerHTML = `Active Portal: <strong style="color: var(--primary-bright);">🛡️ Parent Login Portal</strong>`;
    }
    if (submitText) {
      submitText.textContent = "Sign In as Parent";
    }
  }
}

// Role selection in Register view
function selectRegisterRole(role) {
  currentRegisterRole = role;
  updateRegisterPortalUI();
}

function updateRegisterPortalUI() {
  const studentCard = document.getElementById("reg-role-student");
  const parentCard = document.getElementById("reg-role-parent");
  const roleInput = document.getElementById("register-role");
  const submitText = document.getElementById("reg-submit-text");
  const guidanceText = document.getElementById("reg-guidance-text");

  if (!studentCard || !parentCard) return;

  roleInput.value = currentRegisterRole;

  if (currentRegisterRole === "STUDENT") {
    studentCard.classList.add("active");
    parentCard.classList.remove("active");
    if (submitText) submitText.textContent = "Create Student Account";
    if (guidanceText) {
      guidanceText.textContent = "Student accounts receive full autonomous budgeting and a personal family invitation code to share with parents.";
    }
  } else {
    parentCard.classList.add("active");
    studentCard.classList.remove("active");
    if (submitText) submitText.textContent = "Create Parent Account";
    if (guidanceText) {
      guidanceText.textContent = "Parent accounts operate in read-only mode to monitor approved student expenses without modifying records.";
    }
  }
}

// Password visibility toggle
function togglePasswordVisibility(inputId, btn) {
  const input = document.getElementById(inputId);
  if (!input) return;

  const isPassword = input.type === "password";
  input.type = isPassword ? "text" : "password";

  const eyeOpen = btn.querySelector(".eye-open");
  const eyeClosed = btn.querySelector(".eye-closed");
  if (eyeOpen && eyeClosed) {
    eyeOpen.style.display = isPassword ? "none" : "";
    eyeClosed.style.display = isPassword ? "" : "none";
  }
}

// Open Forgot Password
function openForgotPasswordView() {
  const loginEmail = document.getElementById("login-email") ? document.getElementById("login-email").value.trim() : "";
  if (loginEmail && document.getElementById("forgot-email")) {
    document.getElementById("forgot-email").value = loginEmail;
  }
  if (document.getElementById("forgot-role")) {
    document.getElementById("forgot-role").value = currentLoginRole;
  }
  switchAuthMode("forgot");
}

// Handle Login Submit
async function handleLoginSubmit(e) {
  e.preventDefault();
  const email = document.getElementById("login-email").value.trim();
  const password = document.getElementById("login-password").value;
  const rememberMeEl = document.getElementById("login-remember-me");
  const rememberMe = rememberMeEl ? rememberMeEl.checked : true;

  const submitBtn = document.getElementById("btn-login-submit");
  const submitText = document.getElementById("login-submit-text");
  const spinner = document.getElementById("login-spinner");

  try {
    submitBtn.disabled = true;
    if (submitText) submitText.style.display = "none";
    if (spinner) spinner.style.display = "inline-block";

    const payload = {
      email,
      password,
      expectedRole: currentLoginRole,
      rememberMe
    };

    const res = await apiRequest("/auth/login", "POST", payload);
    setSession(res.token, {
      userId: res.userId,
      email: res.email,
      displayName: res.displayName,
      role: res.role,
      invitationCode: res.invitationCode
    }, rememberMe);

    closeModal("auth-modal");
    showToast(`Welcome back, ${res.displayName}! (${res.role} Portal)`, "success");
    await initializeApp();
  } catch (err) {
    // Error is shown via toast in api.js
  } finally {
    submitBtn.disabled = false;
    if (submitText) submitText.style.display = "";
    if (spinner) spinner.style.display = "none";
  }
}

// Handle Register Submit
async function handleRegisterSubmit(e) {
  e.preventDefault();
  const role = document.getElementById("register-role").value;
  const displayName = document.getElementById("register-name").value.trim();
  const email = document.getElementById("register-email").value.trim();
  const password = document.getElementById("register-password").value;
  const confirmPassword = document.getElementById("register-confirm-password").value;

  if (password.length < 6) {
    showToast("Password must be at least 6 characters.", "error");
    return;
  }

  if (password !== confirmPassword) {
    showToast("Passwords do not match. Please re-enter.", "error");
    return;
  }

  const submitBtn = document.getElementById("btn-register-submit");
  const submitText = document.getElementById("reg-submit-text");
  const spinner = document.getElementById("reg-spinner");

  try {
    submitBtn.disabled = true;
    if (submitText) submitText.style.display = "none";
    if (spinner) spinner.style.display = "inline-block";

    const payload = {
      email,
      password,
      displayName,
      role
    };

    const res = await apiRequest("/auth/register", "POST", payload);
    setSession(res.token, {
      userId: res.userId,
      email: res.email,
      displayName: res.displayName,
      role: res.role,
      invitationCode: res.invitationCode
    }, true);

    closeModal("auth-modal");
    showToast(`Account registered successfully! Welcome, ${res.displayName}.`, "success");
    await initializeApp();
  } catch (err) {
    // Handled in api.js
  } finally {
    submitBtn.disabled = false;
    if (submitText) submitText.style.display = "";
    if (spinner) spinner.style.display = "none";
  }
}

// Handle Forgot Password Request (Step 1)
async function handleForgotPasswordRequest(e) {
  e.preventDefault();
  const email = document.getElementById("forgot-email").value.trim();
  const role = document.getElementById("forgot-role").value;

  const btn = document.getElementById("btn-forgot-request");
  const spinner = document.getElementById("forgot-req-spinner");

  try {
    btn.disabled = true;
    if (spinner) spinner.style.display = "inline-block";

    const res = await apiRequest("/auth/forgot-password", "POST", { email, role });

    showToast(res.message || "Reset code generated.", "success");

    // Transition to Step 2
    document.getElementById("forgot-step-1").style.display = "none";
    document.getElementById("forgot-step-2").style.display = "block";
    document.getElementById("reset-email").value = email;

    // Display generated code prominently for convenient verification
    if (res.resetCode) {
      document.getElementById("generated-reset-code").textContent = res.resetCode;
      document.getElementById("reset-code-display-box").style.display = "block";
      document.getElementById("reset-token").value = res.resetCode;
    }
  } catch (err) {
    // Handled in api.js
  } finally {
    btn.disabled = false;
    if (spinner) spinner.style.display = "none";
  }
}

// Handle Reset Password Submit (Step 2)
async function handleResetPasswordSubmit(e) {
  e.preventDefault();
  const email = document.getElementById("reset-email").value.trim();
  const token = document.getElementById("reset-token").value.trim();
  const newPassword = document.getElementById("reset-new-password").value;
  const confirmPassword = document.getElementById("reset-confirm-password").value;

  if (newPassword.length < 6) {
    showToast("New password must be at least 6 characters.", "error");
    return;
  }

  if (newPassword !== confirmPassword) {
    showToast("Passwords do not match.", "error");
    return;
  }

  const btn = document.getElementById("btn-reset-submit");
  const spinner = document.getElementById("reset-submit-spinner");

  try {
    btn.disabled = true;
    if (spinner) spinner.style.display = "inline-block";

    const res = await apiRequest("/auth/reset-password", "POST", { email, token, newPassword });
    showToast(res.message || "Password updated successfully!", "success");

    // Transition back to login with prefilled email
    switchAuthMode("login");
    document.getElementById("login-email").value = email;
    document.getElementById("login-password").value = "";
  } catch (err) {
    // Handled in api.js
  } finally {
    btn.disabled = false;
    if (spinner) spinner.style.display = "none";
  }
}

function openAuthModal() {
  switchAuthMode("login");
  openModal("auth-modal");
}

function applyRoleRestrictions(role) {
  const isParent = role === "PARENT";
  const parentBanner = document.getElementById("parent-read-only-banner");
  const roleBadge = document.getElementById("header-role-badge");

  if (isParent) {
    if (parentBanner) parentBanner.style.display = "flex";
    if (roleBadge) {
      roleBadge.textContent = "PARENT MODE (READ-ONLY)";
      roleBadge.className = "role-badge parent";
    }

    // Disable or hide mutation buttons for Parent
    document.querySelectorAll(".student-only").forEach(el => {
      el.style.display = "none";
    });
  } else {
    if (parentBanner) parentBanner.style.display = "none";
    if (roleBadge) {
      roleBadge.textContent = "STUDENT MODE";
      roleBadge.className = "role-badge student";
    }

    // Show student controls
    document.querySelectorAll(".student-only").forEach(el => {
      el.style.display = "";
    });
  }
}

async function initializeApp() {
  const stored = getStoredUser();
  if (!stored || !getToken()) {
    const nameEl = document.getElementById("user-display-name");
    const emailEl = document.getElementById("user-email");
    if (nameEl) nameEl.textContent = "Guest User";
    if (emailEl) emailEl.textContent = "Please sign in";
    openAuthModal();
    return;
  }

  currentUser = stored;
  const nameEl = document.getElementById("user-display-name");
  const emailEl = document.getElementById("user-email");
  if (nameEl) nameEl.textContent = currentUser.displayName;
  if (emailEl) emailEl.textContent = currentUser.email;

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

  const pageTitle = document.getElementById("page-title");
  if (pageTitle) pageTitle.textContent = titles[viewId] || "Finance System Manager";

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
const logoutBtn = document.getElementById("btn-logout");
if (logoutBtn) {
  logoutBtn.addEventListener("click", () => {
    clearSession();
    currentUser = null;
    const nameEl = document.getElementById("user-display-name");
    const emailEl = document.getElementById("user-email");
    if (nameEl) nameEl.textContent = "Guest User";
    if (emailEl) emailEl.textContent = "Please sign in";
    showToast("Logged out successfully.", "info");
    openAuthModal();
  });
}

// CSV Export
const csvBtn = document.getElementById("btn-export-csv");
if (csvBtn) {
  csvBtn.addEventListener("click", async () => {
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
}

// Boot application on DOM ready
document.addEventListener("DOMContentLoaded", () => {
  setupMobileNav();
  initializeApp();
});
