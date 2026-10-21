// ==============================================================
// Transaction Management (CRUD & Filters)
// ==============================================================

let studentCategories = [
  "Food & Mess",
  "Hostel & Rent",
  "Books & Supplies",
  "College Fees & Tuition",
  "Transport & Commute",
  "Subscriptions & Tools",
  "Personal & Entertainment",
  "Allowance",
  "Stipend",
  "Other"
];

let pendingDeleteTxId = null;

async function loadCategories() {
  try {
    const list = await apiRequest("/intelligence/categories");
    if (list && list.length > 0) {
      studentCategories = list;
    }
  } catch (ignored) {}

  populateCategoryDropdowns();
}

function populateCategoryDropdowns() {
  const filterSelect = document.getElementById("filter-tx-category");
  const modalSelect = document.getElementById("tx-category");
  const budgetSelect = document.getElementById("budget-category");

  if (filterSelect) {
    filterSelect.innerHTML = `<option value="">All Categories</option>`;
    studentCategories.forEach(cat => {
      filterSelect.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
    });
  }

  if (modalSelect) {
    modalSelect.innerHTML = "";
    studentCategories.forEach(cat => {
      modalSelect.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
    });
  }

  if (budgetSelect) {
    budgetSelect.innerHTML = "";
    studentCategories.forEach(cat => {
      budgetSelect.innerHTML += `<option value="${escapeHtml(cat)}">${escapeHtml(cat)}</option>`;
    });
  }
}

async function loadTransactions() {
  const category = document.getElementById("filter-tx-category").value;
  const type = document.getElementById("filter-tx-type").value;
  const startDate = document.getElementById("filter-tx-start").value;
  const endDate = document.getElementById("filter-tx-end").value;

  const params = new URLSearchParams();
  if (category) params.append("category", category);
  if (type) params.append("type", type);
  if (startDate) params.append("startDate", startDate);
  if (endDate) params.append("endDate", endDate);

  if (currentUser && currentUser.role === "PARENT") {
    const parentStudentId = localStorage.getItem("parent_selected_student_id");
    if (parentStudentId) params.append("studentId", parentStudentId);
  }

  try {
    const list = await apiRequest(`/transactions?${params.toString()}`);
    renderTransactionsTable(list || []);
  } catch (err) {
    console.error("Failed to load transactions:", err);
  }
}

function renderTransactionsTable(list) {
  const tbody = document.getElementById("transactions-table-tbody");
  tbody.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="${isParent ? 6 : 7}" style="text-align: center; color: var(--text-muted); padding: 32px;">No transactions found matching your criteria.</td></tr>`;
    return;
  }

  list.forEach(tx => {
    const tr = document.createElement("tr");
    const isIncome = tx.type === "INCOME";

    let actionsHtml = "";
    if (!isParent) {
      actionsHtml = `
        <td class="student-only">
          <button class="btn btn-secondary btn-sm" onclick="openEditTxModal(${JSON.stringify(tx).replace(/"/g, '&quot;')})">Edit</button>
          <button class="btn btn-danger btn-sm" onclick="confirmDeleteTx(${tx.id})">Delete</button>
        </td>
      `;
    }

    tr.innerHTML = `
      <td>${formatDate(tx.transactionDate)}</td>
      <td><span class="badge ${isIncome ? 'badge-income' : 'badge-expense'}">${tx.type}</span></td>
      <td><strong>${escapeHtml(tx.category)}</strong></td>
      <td>${escapeHtml(tx.description)}</td>
      <td style="font-weight: 700; color: ${isIncome ? 'var(--success)' : 'var(--text-main)'};">
        ${isIncome ? '+' : '-'} ${formatCurrency(tx.amount)}
      </td>
      <td>${escapeHtml(tx.paymentMethod || 'Manual')}</td>
      ${actionsHtml}
    `;
    tbody.appendChild(tr);
  });
}

function resetTxFilters() {
  document.getElementById("filter-tx-category").value = "";
  document.getElementById("filter-tx-type").value = "";
  document.getElementById("filter-tx-start").value = "";
  document.getElementById("filter-tx-end").value = "";
  loadTransactions();
}

function openAddTxModal() {
  document.getElementById("tx-form").reset();
  document.getElementById("tx-id").value = "";
  document.getElementById("tx-modal-title").textContent = "Add Transaction";
  document.getElementById("tx-date").value = new Date().toISOString().split("T")[0];
  openModal("tx-modal");
}

function openEditTxModal(tx) {
  document.getElementById("tx-id").value = tx.id;
  document.getElementById("tx-modal-title").textContent = "Edit Transaction";
  document.getElementById("tx-type").value = tx.type;
  document.getElementById("tx-amount").value = tx.amount;
  document.getElementById("tx-category").value = tx.category;
  document.getElementById("tx-date").value = tx.transactionDate;
  document.getElementById("tx-description").value = tx.description;
  document.getElementById("tx-payment-method").value = tx.paymentMethod || "UPI";
  openModal("tx-modal");
}

async function handleTransactionSubmit(e) {
  e.preventDefault();
  const id = document.getElementById("tx-id").value;
  const dto = {
    type: document.getElementById("tx-type").value,
    amount: document.getElementById("tx-amount").value,
    category: document.getElementById("tx-category").value,
    transactionDate: document.getElementById("tx-date").value,
    description: document.getElementById("tx-description").value.trim(),
    paymentMethod: document.getElementById("tx-payment-method").value
  };

  try {
    if (id) {
      await apiRequest(`/transactions/${id}`, "PUT", dto);
      showToast("Transaction updated successfully.", "success");
    } else {
      await apiRequest("/transactions", "POST", dto);
      showToast("Transaction added successfully.", "success");
    }
    closeModal("tx-modal");
    loadTransactions();
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

function confirmDeleteTx(id) {
  pendingDeleteTxId = id;
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to permanently delete this transaction?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/transactions/${pendingDeleteTxId}`, "DELETE");
      showToast("Transaction deleted successfully.", "success");
      closeModal("delete-modal");
      loadTransactions();
      loadDashboard();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}

// Global hook for top header quick-add button
document.getElementById("btn-open-add-tx").addEventListener("click", () => {
  openAddTxModal();
});
