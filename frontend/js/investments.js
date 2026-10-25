// ==============================================================
// Investment Portfolio Management
// ==============================================================

async function loadInvestments() {
  try {
    const list = await apiRequest("/investments");
    renderInvestments(list || []);
  } catch (err) {
    console.error("Failed to load investments:", err);
  }
}

function renderInvestments(list) {
  const tbody = document.getElementById("investments-tbody");
  tbody.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="${isParent ? 7 : 8}" style="text-align: center; color: var(--text-muted); padding: 32px;">No investment assets recorded yet.</td></tr>`;
    return;
  }

  list.forEach(inv => {
    const tr = document.createElement("tr");
    const isProfit = Number(inv.profitLoss || 0) >= 0;

    let actionsHtml = "";
    if (!isParent) {
      actionsHtml = `
        <td class="student-only">
          <button class="btn btn-secondary btn-sm" onclick="openEditInvestmentModal(${JSON.stringify(inv).replace(/"/g, '&quot;')})">Update</button>
          <button class="btn btn-danger btn-sm" onclick="deleteInvestment(${inv.id})">Delete</button>
        </td>
      `;
    }

    tr.innerHTML = `
      <td><span class="badge" style="background: rgba(212, 175, 55, 0.12); color: var(--electric-blue); border: 1px solid rgba(212, 175, 55, 0.3);">${escapeHtml(inv.assetType)}</span></td>
      <td><strong>${escapeHtml(inv.assetName)}</strong></td>
      <td>${formatCurrency(inv.investedAmount)}</td>
      <td><strong>${formatCurrency(inv.currentValue)}</strong></td>
      <td style="font-weight: 700; color: ${isProfit ? 'var(--success)' : 'var(--danger)'};">
        ${isProfit ? '+' : ''}${formatCurrency(inv.profitLoss)}
      </td>
      <td>
        <span class="badge" style="background: ${isProfit ? 'var(--success-light)' : 'var(--danger-light)'}; color: ${isProfit ? 'var(--success)' : 'var(--danger)'};">
          ${isProfit ? '+' : ''}${inv.profitLossPercentage}%
        </span>
      </td>
      <td>${formatDate(inv.lastUpdated)}</td>
      ${actionsHtml}
    `;
    tbody.appendChild(tr);
  });
}

function openInvestmentModal() {
  document.getElementById("inv-form").reset();
  document.getElementById("inv-id").value = "";
  document.getElementById("inv-modal-title").textContent = "Add Investment Asset";
  openModal("investment-modal");
}

function openEditInvestmentModal(inv) {
  document.getElementById("inv-id").value = inv.id;
  document.getElementById("inv-modal-title").textContent = "Update Valuation";
  document.getElementById("inv-type").value = inv.assetType;
  document.getElementById("inv-name").value = inv.assetName;
  document.getElementById("inv-invested").value = inv.investedAmount;
  document.getElementById("inv-current").value = inv.currentValue;
  openModal("investment-modal");
}

async function handleInvestmentSubmit(e) {
  e.preventDefault();
  const id = document.getElementById("inv-id").value;
  const dto = {
    assetType: document.getElementById("inv-type").value,
    assetName: document.getElementById("inv-name").value.trim(),
    investedAmount: document.getElementById("inv-invested").value,
    currentValue: document.getElementById("inv-current").value
  };

  try {
    if (id) {
      await apiRequest(`/investments/${id}`, "PUT", dto);
      showToast("Investment valuation updated.", "success");
    } else {
      await apiRequest("/investments", "POST", dto);
      showToast("Investment asset registered.", "success");
    }
    closeModal("investment-modal");
    loadInvestments();
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

async function deleteInvestment(id) {
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to remove this investment asset?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/investments/${id}`, "DELETE");
      showToast("Investment removed.", "success");
      closeModal("delete-modal");
      loadInvestments();
      loadDashboard();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}
