// ==============================================================
// Dashboard Analytics & Visualizations
// ==============================================================

async function loadDashboard(selectedStudentId) {
  try {
    const isParentRole = currentUser && currentUser.role === "PARENT";
    let url = "/reports/dashboard";
    
    if (isParentRole) {
      const activeStudentId = selectedStudentId || localStorage.getItem("parent_selected_student_id");
      if (activeStudentId) {
        url += `?studentId=${encodeURIComponent(activeStudentId)}`;
      }
    }

    const summary = await apiRequest(url);
    if (!summary) return;

    const isParent = summary.userRole === "PARENT";
    const unlinkedAlert = document.getElementById("parent-unlinked-alert");
    const parentBadge = document.getElementById("parent-link-badge");
    const parentSelect = document.getElementById("parent-student-select");

    // Set KPI cards
    const incomeEl = document.getElementById("metric-income");
    const expenseEl = document.getElementById("metric-expense");
    const netEl = document.getElementById("metric-net");
    const balanceEl = document.getElementById("metric-balance");

    if (isParent) {
      const isUnlinked = summary.linkStatus === "UNLINKED" || !summary.studentId;

      if (unlinkedAlert) {
        unlinkedAlert.style.display = isUnlinked ? "flex" : "none";
      }

      if (parentBadge) {
        parentBadge.textContent = isUnlinked ? "NO ACTIVE LINK" : "LINK ACTIVE";
        parentBadge.className = isUnlinked ? "badge badge-revoked" : "badge badge-active";
      }

      // Populate student selector if available
      if (parentSelect && summary.availableStudents) {
        parentSelect.innerHTML = "";
        if (summary.availableStudents.length === 0) {
          parentSelect.innerHTML = `<option value="">No Active Students</option>`;
        } else {
          summary.availableStudents.forEach(stu => {
            const opt = document.createElement("option");
            opt.value = stu.studentId;
            opt.textContent = `${stu.studentName} (${stu.studentEmail})`;
            if (stu.studentId === summary.studentId) {
              opt.selected = true;
            }
            parentSelect.appendChild(opt);
          });
        }
      }

      if (summary.studentId) {
        localStorage.setItem("parent_selected_student_id", summary.studentId);
      }

      incomeEl.textContent = "Private (Student Only)";
      incomeEl.style.fontSize = "16px";
      document.getElementById("metric-income-sub").textContent = "Read-only mode";

      const totalExp = summary.totalExpenses || 0;
      expenseEl.textContent = formatCurrency(totalExp);
      netEl.textContent = "- " + formatCurrency(totalExp);
      netEl.style.color = "var(--text-main)";
      balanceEl.textContent = "N/A (Read-only)";

    } else {
      if (unlinkedAlert) unlinkedAlert.style.display = "none";

      incomeEl.textContent = formatCurrency(summary.totalIncome || 0);
      incomeEl.style.fontSize = "26px";
      document.getElementById("metric-income-sub").textContent = "Total incoming";

      expenseEl.textContent = formatCurrency(summary.totalExpenses || 0);
      netEl.textContent = formatCurrency(summary.netCashFlow || 0);
      netEl.style.color = Number(summary.netCashFlow || 0) >= 0 ? "var(--success)" : "var(--danger)";
      balanceEl.textContent = formatCurrency(summary.trackedBalance || 0);
    }

    // Render Recent Transactions
    renderRecentTransactions(summary.recentTransactions || []);

    // Render Category Chart
    renderCategoryDonutChart(summary.categoryExpenses || {});

    // Render Monthly Trends Bar Chart
    renderTrendsBarChart(summary.monthlyTrends || [], isParent);

  } catch (err) {
    console.error("Failed to load dashboard:", err);
  }
}

function onParentStudentSelected(studentId) {
  if (studentId) {
    localStorage.setItem("parent_selected_student_id", studentId);
    loadDashboard(studentId);
    loadTransactions();
  }
}

function renderRecentTransactions(list) {
  const tbody = document.getElementById("recent-transactions-tbody");
  tbody.innerHTML = "";

  if (list.length === 0) {
    tbody.innerHTML = `<tr><td colspan="6" style="text-align: center; color: var(--text-muted); padding: 24px;">No recent transactions found.</td></tr>`;
    return;
  }

  list.forEach(tx => {
    const tr = document.createElement("tr");
    const isIncome = tx.type === "INCOME";
    tr.innerHTML = `
      <td>${formatDate(tx.transactionDate)}</td>
      <td><span class="badge ${isIncome ? 'badge-income' : 'badge-expense'}">${tx.type}</span></td>
      <td><strong>${escapeHtml(tx.category)}</strong></td>
      <td>${escapeHtml(tx.description)}</td>
      <td style="font-weight: 700; color: ${isIncome ? 'var(--success)' : 'var(--text-main)'};">
        ${isIncome ? '+' : '-'} ${formatCurrency(tx.amount)}
      </td>
      <td>${escapeHtml(tx.paymentMethod || 'Manual')}</td>
    `;
    tbody.appendChild(tr);
  });
}

function renderCategoryDonutChart(categoryMap) {
  const container = document.getElementById("category-chart-container");
  const entries = Object.entries(categoryMap);

  if (entries.length === 0) {
    container.innerHTML = `<div style="color: var(--text-secondary); font-size: 13px; text-align: center;">No expense categories recorded yet.</div>`;
    return;
  }

  const colors = [
    "#D4AF37", "#F5D77A", "#B8860B", "#10B981",
    "#E5C158", "#8F6B18", "#E2E8F0", "#F43F5E", "#94A3B8"
  ];

  const total = entries.reduce((acc, curr) => acc + Number(curr[1]), 0);

  // Build SVG Pie / Donut
  let cumulativePercent = 0;
  const slices = entries.map(([cat, val], idx) => {
    const percent = (Number(val) / total) * 100;
    const startAngle = (cumulativePercent / 100) * 360;
    cumulativePercent += percent;
    const endAngle = (cumulativePercent / 100) * 360;

    return {
      cat,
      val: Number(val),
      percent: percent.toFixed(1),
      color: colors[idx % colors.length],
      startAngle,
      endAngle
    };
  });

  // Render SVG Paths
  let paths = "";
  slices.forEach(s => {
    const startX = 100 + 70 * Math.cos((Math.PI * (s.startAngle - 90)) / 180);
    const startY = 100 + 70 * Math.sin((Math.PI * (s.startAngle - 90)) / 180);
    const endX = 100 + 70 * Math.cos((Math.PI * (s.endAngle - 90)) / 180);
    const endY = 100 + 70 * Math.sin((Math.PI * (s.endAngle - 90)) / 180);

    const largeArcFlag = s.endAngle - s.startAngle > 180 ? 1 : 0;
    const d = `M 100 100 L ${startX} ${startY} A 70 70 0 ${largeArcFlag} 1 ${endX} ${endY} Z`;
    paths += `<path d="${d}" fill="${s.color}" stroke="rgba(16, 17, 20, 0.95)" stroke-width="2"><title>${s.cat}: ${formatCurrency(s.val)} (${s.percent}%)</title></path>`;
  });

  // SVG inner cutout for Donut with luxury black-gold glass center
  paths += `<circle cx="100" cy="100" r="44" fill="rgba(16, 17, 20, 0.95)" stroke="rgba(212, 175, 55, 0.3)" stroke-width="1.5" />`;
  paths += `<text x="100" y="95" text-anchor="middle" font-size="9" fill="#A5A5AD" font-weight="700" letter-spacing="1">TOTAL</text>`;
  paths += `<text x="100" y="112" text-anchor="middle" font-size="12" fill="#F5D77A" font-weight="800">${formatCurrency(total)}</text>`;

  // Legend
  let legendHtml = `<div style="display: flex; flex-wrap: wrap; gap: 8px 14px; margin-top: 14px; justify-content: center;">`;
  slices.forEach(s => {
    legendHtml += `
      <div style="display: flex; align-items: center; gap: 6px; font-size: 12px;">
        <span style="width: 10px; height: 10px; border-radius: 3px; background: ${s.color}; display: inline-block; box-shadow: 0 0 6px ${s.color};"></span>
        <span style="color: var(--text-main); font-weight: 500;">${escapeHtml(s.cat)}:</span>
        <span style="color: var(--text-secondary);">${s.percent}%</span>
      </div>
    `;
  });
  legendHtml += `</div>`;

  container.innerHTML = `
    <svg width="200" height="200" viewBox="0 0 200 200" style="overflow: visible;">
      ${paths}
    </svg>
    ${legendHtml}
  `;
}

function renderTrendsBarChart(trends, isParent) {
  const container = document.getElementById("trends-chart-container");

  if (!trends || trends.length === 0) {
    container.innerHTML = `<div style="color: var(--text-secondary); font-size: 13px; text-align: center; padding: 40px 0;">No monthly trend data recorded.</div>`;
    return;
  }

  // Find max value for scaling
  let maxVal = 1000;
  trends.forEach(t => {
    const exp = Number(t.expense || 0);
    const inc = isParent ? 0 : Number(t.income || 0);
    if (exp > maxVal) maxVal = exp;
    if (inc > maxVal) maxVal = inc;
  });

  const chartHeight = 160;
  const chartWidth = 420;
  const barWidth = 18;
  const gap = 38;

  let svgBars = "";
  let xLabels = "";

  trends.forEach((t, i) => {
    const x = 30 + i * (barWidth * 2 + gap);
    const expHeight = Math.max(3, (Number(t.expense || 0) / maxVal) * chartHeight);
    const expY = chartHeight - expHeight + 20;

    // Expense bar (Refined Rose)
    svgBars += `
      <rect x="${x}" y="${expY}" width="${barWidth}" height="${expHeight}" fill="#F43F5E" rx="4">
        <title>Expense (${t.monthLabel}): ${formatCurrency(t.expense)}</title>
      </rect>
    `;

    // Income bar (Refined Emerald - only shown if not in parent view)
    if (!isParent) {
      const incHeight = Math.max(3, (Number(t.income || 0) / maxVal) * chartHeight);
      const incY = chartHeight - incHeight + 20;
      svgBars += `
        <rect x="${x + barWidth + 3}" y="${incY}" width="${barWidth}" height="${incHeight}" fill="#10B981" rx="4">
          <title>Income (${t.monthLabel}): ${formatCurrency(t.income)}</title>
        </rect>
      `;
    }

    xLabels += `
      <text x="${x + barWidth}" y="${chartHeight + 38}" text-anchor="middle" font-size="10" fill="#A5A5AD">${t.monthLabel}</text>
    `;
  });

  const legend = isParent ? `
    <div style="display: flex; gap: 14px; margin-top: 10px; font-size: 12px; justify-content: center;">
      <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 12px; height: 12px; background: #F43F5E; border-radius: 3px; box-shadow: 0 0 8px rgba(244,63,94,0.4);"></span> Expenses</div>
    </div>
  ` : `
    <div style="display: flex; gap: 14px; margin-top: 10px; font-size: 12px; justify-content: center;">
      <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 12px; height: 12px; background: #F43F5E; border-radius: 3px; box-shadow: 0 0 8px rgba(244,63,94,0.4);"></span> Expenses</div>
      <div style="display: flex; align-items: center; gap: 6px;"><span style="width: 12px; height: 12px; background: #10B981; border-radius: 3px; box-shadow: 0 0 8px rgba(16,185,129,0.4);"></span> Income</div>
    </div>
  `;

  container.innerHTML = `
    <svg width="100%" height="200" viewBox="0 0 ${chartWidth} 200" preserveAspectRatio="xMidYMid meet">
      <!-- Grid lines -->
      <line x1="20" y1="20" x2="${chartWidth}" y2="20" stroke="rgba(212, 175, 55, 0.08)" stroke-width="1" />
      <line x1="20" y1="${chartHeight / 2 + 20}" x2="${chartWidth}" y2="${chartHeight / 2 + 20}" stroke="rgba(212, 175, 55, 0.08)" stroke-width="1" />
      <line x1="20" y1="${chartHeight + 20}" x2="${chartWidth}" y2="${chartHeight + 20}" stroke="rgba(212, 175, 55, 0.16)" stroke-width="1" />
      ${svgBars}
      ${xLabels}
    </svg>
    ${legend}
  `;
}

function escapeHtml(str) {
  if (!str) return "";
  return str.replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;").replace(/"/g, "&quot;");
}
