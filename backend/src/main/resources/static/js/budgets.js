// ==============================================================
// Budget Management & Threshold Alerts
// ==============================================================

async function loadBudgets() {
  try {
    const list = await apiRequest("/budgets");
    renderBudgets(list || []);
  } catch (err) {
    console.error("Failed to load budgets:", err);
  }
}

function renderBudgets(list) {
  const container = document.getElementById("budgets-container");
  container.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (list.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 40px; background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border-radius: var(--radius-lg); border: 1px solid var(--border);">
        <p style="font-size: 14px; margin-bottom: 8px;">No monthly budgets configured yet.</p>
        ${!isParent ? '<button class="btn btn-primary btn-sm" onclick="openBudgetModal()">Create First Budget</button>' : ''}
      </div>
    `;
    return;
  }

  list.forEach(b => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";

    const pct = Math.min(100, Number(b.percentageSpent || 0));
    let fillClass = "";
    let badgeHtml = "";

    if (b.overBudget) {
      fillClass = "over";
      badgeHtml = `<span class="badge" style="background: var(--danger-light); color: var(--danger);">OVER BUDGET (${b.percentageSpent}%)</span>`;
    } else if (b.warningTriggered) {
      fillClass = "warning";
      badgeHtml = `<span class="badge" style="background: var(--warning-light); color: #b45309;">ALERT: NEAR LIMIT (${b.percentageSpent}%)</span>`;
    } else {
      badgeHtml = `<span class="badge" style="background: var(--success-light); color: var(--success);">${b.percentageSpent}% Spent</span>`;
    }

    let deleteBtn = "";
    if (!isParent) {
      deleteBtn = `<button class="btn btn-secondary btn-sm" onclick="deleteBudget(${b.id})" style="padding: 3px 8px; font-size: 11px;">Remove</button>`;
    }

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <div>
          <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(b.category)}</h4>
          <span style="font-size: 11px; color: var(--text-muted);">Period: ${b.periodMonth}</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          ${badgeHtml}
          ${deleteBtn}
        </div>
      </div>
      <div class="card-body" style="padding: 18px;">
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px;">
          <span>Spent: <strong>${formatCurrency(b.actualSpent)}</strong></span>
          <span>Limit: <strong>${formatCurrency(b.limitAmount)}</strong></span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill ${fillClass}" style="width: ${pct}%;"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-top: 8px;">
          <span>Warning threshold: ${b.warningThreshold}%</span>
          <span style="font-weight: 600; color: ${b.remainingAmount < 0 ? 'var(--danger)' : 'var(--text-main)'};">
            ${b.remainingAmount < 0 ? 'Over by ' + formatCurrency(Math.abs(b.remainingAmount)) : 'Remaining: ' + formatCurrency(b.remainingAmount)}
          </span>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function openBudgetModal() {
  document.getElementById("budget-form").reset();
  const currentMonth = new Date().toISOString().slice(0, 7);
  document.getElementById("budget-month").value = currentMonth;
  document.getElementById("budget-warning").value = 80;
  openModal("budget-modal");
}

async function handleBudgetSubmit(e) {
  e.preventDefault();
  const dto = {
    category: document.getElementById("budget-category").value,
    periodMonth: document.getElementById("budget-month").value,
    limitAmount: document.getElementById("budget-limit").value,
    warningThreshold: document.getElementById("budget-warning").value
  };

  try {
    await apiRequest("/budgets", "POST", dto);
    showToast("Budget saved successfully.", "success");
    closeModal("budget-modal");
    loadBudgets();
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

async function deleteBudget(id) {
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to remove this budget?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/budgets/${id}`, "DELETE");
      showToast("Budget removed successfully.", "success");
      closeModal("delete-modal");
      loadBudgets();
      loadDashboard();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}
