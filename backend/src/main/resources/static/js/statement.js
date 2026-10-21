// ==============================================================
// Smart Statement Intelligence (CSV Import & Duplicate Detection)
// ==============================================================

let statementParsedRows = [];

document.addEventListener("DOMContentLoaded", () => {
  const uploadBox = document.getElementById("statement-upload-box");
  const fileInput = document.getElementById("statement-file-input");

  if (uploadBox && fileInput) {
    uploadBox.addEventListener("click", () => fileInput.click());

    fileInput.addEventListener("change", (e) => {
      if (e.target.files.length > 0) {
        uploadStatementFile(e.target.files[0]);
      }
    });

    uploadBox.addEventListener("dragover", (e) => {
      e.preventDefault();
      uploadBox.style.borderColor = "var(--electric-blue)";
      uploadBox.style.background = "rgba(37, 99, 255, 0.15)";
    });

    uploadBox.addEventListener("dragleave", () => {
      uploadBox.style.borderColor = "rgba(77, 141, 255, 0.25)";
      uploadBox.style.background = "rgba(10, 16, 36, 0.6)";
    });

    uploadBox.addEventListener("drop", (e) => {
      e.preventDefault();
      uploadBox.style.borderColor = "rgba(77, 141, 255, 0.25)";
      uploadBox.style.background = "rgba(10, 16, 36, 0.6)";
      if (e.dataTransfer.files.length > 0) {
        uploadStatementFile(e.dataTransfer.files[0]);
      }
    });
  }
});

async function uploadStatementFile(file) {
  const ext = file.name.toLowerCase();
  if (!ext.endsWith(".csv") && !ext.endsWith(".pdf")) {
    showToast("Please select a valid CSV or PDF bank statement file.", "warning");
    return;
  }

  const formData = new FormData();
  formData.append("file", file);

  const fileTypeLabel = ext.endsWith(".pdf") ? "PDF" : "CSV";
  showToast(`Analyzing ${fileTypeLabel} statement & detecting duplicates...`, "info");

  try {
    const preview = await apiRequest("/statements/upload", "POST", formData, true);
    renderStatementPreview(preview);
  } catch (err) {
    // Handled in api.js
  }
}

function renderStatementPreview(preview) {
  statementParsedRows = preview.rows || [];

  const previewSec = document.getElementById("statement-preview-section");
  const summaryEl = document.getElementById("statement-preview-summary");
  const tbody = document.getElementById("statement-preview-tbody");

  previewSec.style.display = "block";
  summaryEl.textContent = `Analyzed ${preview.totalRows} rows: ${preview.validRows} valid new records, ${preview.duplicateRows} suspected duplicates flagged.`;

  tbody.innerHTML = "";
  if (statementParsedRows.length === 0) {
    tbody.innerHTML = `<tr><td colspan="10" style="text-align: center; color: var(--text-muted); padding: 24px;">No valid data rows found in this file.</td></tr>`;
    return;
  }

  statementParsedRows.forEach((row, idx) => {
    const tr = document.createElement("tr");
    if (row.duplicate) {
      tr.style.background = "rgba(255, 93, 115, 0.12)";
    }

    const dupBadge = row.duplicate ?
      `<span class="badge" style="background: var(--danger-light); color: var(--danger);" title="${escapeHtml(row.duplicateReason)}">DUPLICATE FLAGGED</span>` :
      `<span class="badge badge-active">NEW</span>`;

    const debitDisplay = row.debit != null && Number(row.debit) > 0 ? formatCurrency(row.debit) : "-";
    const creditDisplay = row.credit != null && Number(row.credit) > 0 ? formatCurrency(row.credit) : "-";
    const balanceDisplay = row.balance != null ? formatCurrency(row.balance) : "-";

    tr.innerHTML = `
      <td>
        <input type="checkbox" class="import-row-checkbox" data-index="${idx}" ${row.selected ? 'checked' : ''} onchange="updateRowSelection(${idx}, this.checked)">
      </td>
      <td>#${row.rowIndex}</td>
      <td>${formatDate(row.transactionDate)}</td>
      <td>${escapeHtml(row.description)}</td>
      <td><span class="badge ${row.type === 'INCOME' ? 'badge-income' : 'badge-expense'}">${row.type}</span></td>
      <td style="color: var(--danger); font-weight: 600;">${debitDisplay}</td>
      <td style="color: var(--success); font-weight: 600;">${creditDisplay}</td>
      <td style="color: var(--text-secondary);">${balanceDisplay}</td>
      <td><strong>${escapeHtml(row.category)}</strong></td>
      <td>${dupBadge} <span style="font-size: 11px; color: var(--text-muted);">${row.duplicate ? escapeHtml(row.duplicateReason) : ''}</span></td>
    `;
    tbody.appendChild(tr);
  });
}

function updateRowSelection(index, checked) {
  if (statementParsedRows[index]) {
    statementParsedRows[index].selected = checked;
  }
}

function toggleAllImportRows(checked) {
  statementParsedRows.forEach(r => r.selected = checked);
  document.querySelectorAll(".import-row-checkbox").forEach(cb => cb.checked = checked);
}

async function commitStatementImport() {
  const selectedRows = statementParsedRows.filter(r => r.selected);
  if (selectedRows.length === 0) {
    showToast("Please select at least one record to import.", "warning");
    return;
  }

  try {
    const res = await apiRequest("/statements/commit", "POST", selectedRows);
    showToast(res.message, "success");
    document.getElementById("statement-preview-section").style.display = "none";
    statementParsedRows = [];
    document.getElementById("statement-file-input").value = "";
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

function downloadSampleCsv() {
  const sampleContent =
`Date,Type,Amount,Description,Category,Payment Method
2026-10-15,EXPENSE,180.00,Campus Stationery Notebook,Books & Supplies,Cash
2026-10-16,EXPENSE,65.00,Campus Canteen Coffee,Food & Mess,UPI
2026-10-17,INCOME,2500.00,Freelance Python Scripting,Stipend,Bank Transfer
2026-10-18,EXPENSE,450.00,Hostel Laundry Charges,Hostel & Rent,UPI`;

  const blob = new Blob([sampleContent], { type: "text/csv;charset=utf-8;" });
  const url = window.URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = "sample_student_finance_statement.csv";
  document.body.appendChild(a);
  a.click();
  a.remove();
  showToast("Sample statement CSV downloaded.", "info");
}
