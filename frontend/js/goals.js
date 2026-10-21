// ==============================================================
// Savings Goals Management
// ==============================================================

async function loadGoals() {
  try {
    const list = await apiRequest("/goals");
    renderGoals(list || []);
  } catch (err) {
    console.error("Failed to load goals:", err);
  }
}

function renderGoals(list) {
  const container = document.getElementById("goals-container");
  container.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (list.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 40px; background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border-radius: var(--radius-lg); border: 1px solid var(--border);">
        <p style="font-size: 14px; margin-bottom: 8px;">No savings goals created yet.</p>
        ${!isParent ? '<button class="btn btn-primary btn-sm" onclick="openGoalModal()">Set First Goal</button>' : ''}
      </div>
    `;
    return;
  }

  list.forEach(g => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";

    const isComplete = g.status === "COMPLETED" || Number(g.savedAmount) >= Number(g.targetAmount);
    const pct = Math.min(100, Number(g.progressPercentage || 0));

    let actionBtns = "";
    if (!isParent) {
      actionBtns = `
        <div style="display: flex; gap: 6px; margin-top: 14px;">
          <button class="btn btn-secondary btn-sm" onclick="quickAddSavings(${g.id}, ${g.savedAmount}, ${g.targetAmount})" style="flex: 1;">+ Add Funds</button>
          <button class="btn btn-secondary btn-sm" onclick="openEditGoalModal(${JSON.stringify(g).replace(/"/g, '&quot;')})">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="deleteGoal(${g.id})">Delete</button>
        </div>
      `;
    }

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(g.name)}</h4>
        <span class="badge ${isComplete ? 'badge-active' : 'badge-pending'}">
          ${isComplete ? 'COMPLETED' : pct + '% REACHED'}
        </span>
      </div>
      <div class="card-body" style="padding: 18px;">
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 6px;">
          <span>Saved: <strong style="color: var(--success);">${formatCurrency(g.savedAmount)}</strong></span>
          <span>Target: <strong>${formatCurrency(g.targetAmount)}</strong></span>
        </div>
        <div class="progress-bar-bg">
          <div class="progress-bar-fill" style="width: ${pct}%; background: ${isComplete ? 'var(--success)' : 'var(--primary)'};"></div>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 11px; color: var(--text-muted); margin-top: 8px;">
          <span>Target Date: ${formatDate(g.targetDate)}</span>
          <span>Remaining: ${formatCurrency(g.remainingAmount)}</span>
        </div>
        ${actionBtns}
      </div>
    `;
    container.appendChild(card);
  });
}

function openGoalModal() {
  document.getElementById("goal-form").reset();
  document.getElementById("goal-id").value = "";
  document.getElementById("goal-modal-title").textContent = "New Savings Goal";
  openModal("goal-modal");
}

function openEditGoalModal(g) {
  document.getElementById("goal-id").value = g.id;
  document.getElementById("goal-modal-title").textContent = "Edit Savings Goal";
  document.getElementById("goal-name").value = g.name;
  document.getElementById("goal-target").value = g.targetAmount;
  document.getElementById("goal-saved").value = g.savedAmount;
  document.getElementById("goal-date").value = g.targetDate || "";
  openModal("goal-modal");
}

async function quickAddSavings(goalId, currentSaved, targetAmount) {
  const addVal = prompt("Enter additional amount to save (₹):", "500");
  if (!addVal || isNaN(addVal) || Number(addVal) <= 0) return;

  const newSaved = Number(currentSaved) + Number(addVal);
  try {
    const list = await apiRequest("/goals");
    const goal = list.find(g => g.id === goalId);
    if (!goal) return;

    goal.savedAmount = newSaved;
    await apiRequest(`/goals/${goalId}`, "PUT", goal);
    showToast(`Added ${formatCurrency(addVal)} to '${goal.name}'!`, "success");
    loadGoals();
  } catch (err) {
    // Handled in api.js
  }
}

async function handleGoalSubmit(e) {
  e.preventDefault();
  const id = document.getElementById("goal-id").value;
  const dto = {
    name: document.getElementById("goal-name").value.trim(),
    targetAmount: document.getElementById("goal-target").value,
    savedAmount: document.getElementById("goal-saved").value,
    targetDate: document.getElementById("goal-date").value || null
  };

  try {
    if (id) {
      await apiRequest(`/goals/${id}`, "PUT", dto);
      showToast("Savings goal updated successfully.", "success");
    } else {
      await apiRequest("/goals", "POST", dto);
      showToast("Savings goal created successfully.", "success");
    }
    closeModal("goal-modal");
    loadGoals();
  } catch (err) {
    // Handled in api.js
  }
}

async function deleteGoal(id) {
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to delete this savings goal?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/goals/${id}`, "DELETE");
      showToast("Savings goal deleted.", "success");
      closeModal("delete-modal");
      loadGoals();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}
