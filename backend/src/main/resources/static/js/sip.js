// ==============================================================
// Systematic Investment Plan (SIP) Tracker, AMFI Research & Advisory Hub
// ==============================================================

let selectedSchemesToCompare = new Set();
let cachedAmfiSchemes = [];
let cachedAssessment = null;
let cachedProfile = null;

async function loadSips() {
  // 1. Load active SIP list
  try {
    const list = await apiRequest("/sips");
    renderSips(list || []);
    const countBadge = document.getElementById("sip-count-badge");
    if (countBadge) countBadge.textContent = (list || []).length;
  } catch (err) {
    console.error("Failed to load SIPs:", err);
  }

  // 2. Load AMFI Research & Personal Options Hub
  loadSipResearchHub();
}

function switchSipSubView(subview) {
  const researchView = document.getElementById("sip-research-subview");
  const activeView = document.getElementById("sip-active-subview");
  const tabResearch = document.getElementById("tab-sip-research");
  const tabActive = document.getElementById("tab-sip-active");

  if (!researchView || !activeView) return;

  if (subview === "research") {
    researchView.style.display = "block";
    activeView.style.display = "none";
    if (tabResearch) { tabResearch.className = "btn btn-primary btn-sm"; }
    if (tabActive) { tabActive.className = "btn btn-secondary btn-sm"; }
  } else {
    researchView.style.display = "none";
    activeView.style.display = "block";
    if (tabResearch) { tabResearch.className = "btn btn-secondary btn-sm"; }
    if (tabActive) { tabActive.className = "btn btn-primary btn-sm"; }
  }
}

async function loadSipResearchHub() {
  try {
    // A. Fetch Affordability Assessment
    const assess = await apiRequest("/investments/research/assessment");
    cachedAssessment = assess;
    renderAffordabilityAssessment(assess);

    // B. Fetch Investor Profile
    const prof = await apiRequest("/investments/research/profile");
    cachedProfile = prof;
    renderInvestorProfile(prof);

    // C. Fetch Personalized Scheme Recommendations
    const recs = await apiRequest("/investments/research/recommendations");
    renderRecommendedSchemes(recs || []);

    // D. Fetch AMFI Scheme Catalog
    const schemes = await apiRequest("/investments/research/schemes");
    cachedAmfiSchemes = schemes || [];
    renderAmfiSchemesTable(cachedAmfiSchemes);

    // E. Initialize Interactive Simulation
    updateSipSimulation();

  } catch (err) {
    console.error("Failed to load SIP research hub:", err);
  }
}

function renderAffordabilityAssessment(a) {
  const container = document.getElementById("affordability-card-body");
  const badge = document.getElementById("affordability-status-badge");
  if (!container) return;

  if (badge) {
    if (a.affordabilityStatus === "HEALTHY") {
      badge.className = "badge badge-active";
      badge.textContent = "✅ Healthy Cash Flow";
    } else if (a.affordabilityStatus === "EMERGENCY_PRIORITY") {
      badge.className = "badge badge-warning";
      badge.textContent = "🛡️ Emergency Reserve Priority";
    } else {
      badge.className = "badge badge-danger";
      badge.textContent = "⚠️ Tight / Deficit";
    }
  }

  let emergencyWarningHtml = "";
  if (!a.emergencyFundAdequate) {
    emergencyWarningHtml = `
      <div style="background: rgba(255, 184, 77, 0.12); border: 1px solid rgba(255, 184, 77, 0.35); border-radius: var(--radius-sm); padding: 10px 14px; margin-bottom: 14px; font-size: 12px; color: var(--warning); line-height: 1.4;">
        <strong>🛡️ Emergency Safety Priority:</strong> Liquid reserve covers <strong>${a.emergencyReserveMonthsCovered.toFixed(1)} months</strong> of essential burn rate.
        Prioritize liquid funds or bank savings before locking into volatile equity SIPs.
      </div>
    `;
  }

  container.innerHTML = `
    ${emergencyWarningHtml}
    <div style="display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-bottom: 14px; font-size: 12px;">
      <div style="background: rgba(10, 16, 36, 0.6); padding: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border);">
        <span style="color: var(--text-muted); display: block;">Avg Monthly Income</span>
        <strong style="font-size: 14px; color: var(--text-primary);">${formatCurrency(a.averageMonthlyIncome)}</strong>
      </div>
      <div style="background: rgba(10, 16, 36, 0.6); padding: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border);">
        <span style="color: var(--text-muted); display: block;">Essential Living Needs</span>
        <strong style="font-size: 14px; color: var(--danger);">${formatCurrency(a.averageMonthlyEssentialExpense)}</strong>
      </div>
      <div style="background: rgba(10, 16, 36, 0.6); padding: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border);">
        <span style="color: var(--text-muted); display: block;">Existing SIPs & Goals</span>
        <strong style="font-size: 14px; color: var(--warning);">${formatCurrency((a.existingMonthlySipCommitments || 0) + (a.monthlySavingsGoalCommitments || 0))}</strong>
      </div>
      <div style="background: rgba(10, 16, 36, 0.6); padding: 10px; border-radius: var(--radius-sm); border: 1px solid var(--border);">
        <span style="color: var(--text-muted); display: block;">Net Available Surplus</span>
        <strong style="font-size: 14px; color: var(--success);">${formatCurrency(a.netMonthlyDiscretionarySurplus)}</strong>
      </div>
    </div>

    <div style="background: rgba(37, 99, 255, 0.08); border: 1px solid rgba(77, 141, 255, 0.25); border-radius: var(--radius-sm); padding: 12px 14px;">
      <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 4px;">
        <span style="font-size: 11px; text-transform: uppercase; color: var(--primary); font-weight: 700; letter-spacing: 0.5px;">Recommended Affordable SIP Range</span>
        <span style="font-size: 11px; color: var(--text-muted);">${escapeHtml(a.assessmentPeriod)}</span>
      </div>
      <div style="font-size: 18px; font-weight: 800; color: var(--text-primary); margin-bottom: 4px;">
        ${formatCurrency(a.conservativeRecommendedSip)} <span style="font-size: 13px; font-weight: 500; color: var(--text-secondary);">to</span> ${formatCurrency(a.recommendedSip)}<span style="font-size: 12px; font-weight: 500; color: var(--text-muted);">/month</span>
      </div>
      <div style="font-size: 11px; color: var(--text-muted);">
        Conservative limit ensures rent, food, and emergency safety buffers remain untouched. Max safe capacity: ${formatCurrency(a.maxPrudentSip)}/mo.
      </div>
    </div>
  `;
}

function renderInvestorProfile(p) {
  const container = document.getElementById("investor-profile-card-body");
  if (!container) return;

  const isComplete = p && p.questionnaireCompleted;
  const isParent = currentUser && currentUser.role === "PARENT";

  let horizonLabel = "Long-Term (3-5 Years)";
  if (p.investmentHorizon === "SHORT_TERM_UNDER_1Y") horizonLabel = "Short-Term (< 1 Year)";
  else if (p.investmentHorizon === "MEDIUM_TERM_1_3Y") horizonLabel = "Medium-Term (1-3 Years)";
  else if (p.investmentHorizon === "VERY_LONG_TERM_5Y_PLUS") horizonLabel = "Multi-Year (5+ Years)";

  let goalLabel = "Wealth Creation & Compounding";
  if (p.primaryGoal === "EMERGENCY_RESERVE") goalLabel = "Build Liquid Emergency Fund";
  else if (p.primaryGoal === "EDUCATION") goalLabel = "Higher Education & Studies";
  else if (p.primaryGoal === "TAX_SAVING") goalLabel = "Tax Deduction (ELSS 80C)";
  else if (p.primaryGoal === "CAREER_CAPITAL") goalLabel = "Career / Venture Capital";

  container.innerHTML = `
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px;">
      <span style="font-size: 13px; color: var(--text-secondary);">Risk Tolerance:</span>
      <span class="badge ${p.riskTolerance === 'AGGRESSIVE' ? 'badge-danger' : (p.riskTolerance === 'CONSERVATIVE' ? 'badge-active' : 'badge-warning')}">
        ${escapeHtml(p.riskTolerance || 'MODERATE')}
      </span>
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 13px;">
      <span style="color: var(--text-secondary);">Target Horizon:</span>
      <strong>${escapeHtml(horizonLabel)}</strong>
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-size: 13px;">
      <span style="color: var(--text-secondary);">Primary Objective:</span>
      <strong style="color: var(--primary);">${escapeHtml(goalLabel)}</strong>
    </div>
    <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; font-size: 13px;">
      <span style="color: var(--text-secondary);">Stated Emergency Savings:</span>
      <strong>${formatCurrency(p.existingEmergencyFund || 0)} (${p.emergencyFundTargetMonths || 3} mo target)</strong>
    </div>

    ${!isComplete && !isParent ? `
      <div style="background: rgba(37, 99, 255, 0.1); border: 1px dashed var(--primary); border-radius: var(--radius-sm); padding: 10px; text-align: center; margin-top: 10px;">
        <span style="font-size: 12px; color: var(--primary); display: block; margin-bottom: 6px;">Profile Incomplete — Using conservative baseline</span>
        <button class="btn btn-primary btn-sm" onclick="openInvestorProfileModal()" style="font-size: 11px; padding: 4px 10px;">Complete Questionnaire</button>
      </div>
    ` : ''}
  `;
}

function openInvestorProfileModal() {
  if (currentUser && currentUser.role === "PARENT") {
    showToast("Parents have strictly read-only access and cannot edit investor profiles.", "warning");
    return;
  }

  if (cachedProfile) {
    document.getElementById("profile-risk-tolerance").value = cachedProfile.riskTolerance || "MODERATE";
    document.getElementById("profile-horizon").value = cachedProfile.investmentHorizon || "LONG_TERM_3_5Y";
    document.getElementById("profile-goal").value = cachedProfile.primaryGoal || "WEALTH_CREATION";
    document.getElementById("profile-emergency-fund").value = cachedProfile.existingEmergencyFund || 0;
    document.getElementById("profile-debt-obligations").value = cachedProfile.monthlyDebtObligations || 0;
    document.getElementById("profile-target-months").value = cachedProfile.emergencyFundTargetMonths || 3;
    document.getElementById("profile-essential-override").value = cachedProfile.manualEssentialExpenseOverride || "";
  }
  openModal("investor-profile-modal");
}

async function handleInvestorProfileSubmit(e) {
  e.preventDefault();
  const dto = {
    riskTolerance: document.getElementById("profile-risk-tolerance").value,
    investmentHorizon: document.getElementById("profile-horizon").value,
    primaryGoal: document.getElementById("profile-goal").value,
    existingEmergencyFund: parseFloat(document.getElementById("profile-emergency-fund").value) || 0,
    monthlyDebtObligations: parseFloat(document.getElementById("profile-debt-obligations").value) || 0,
    emergencyFundTargetMonths: parseInt(document.getElementById("profile-target-months").value) || 3,
    manualEssentialExpenseOverride: document.getElementById("profile-essential-override").value ? parseFloat(document.getElementById("profile-essential-override").value) : null
  };

  try {
    const updated = await apiRequest("/investments/research/profile", "POST", dto);
    showToast("Investor profile updated successfully.", "success");
    closeModal("investor-profile-modal");
    loadSipResearchHub();
  } catch (err) {
    // Handled in api.js
  }
}

function renderRecommendedSchemes(list) {
  const container = document.getElementById("recommended-schemes-grid");
  if (!container) return;
  container.innerHTML = "";

  if (list.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 30px;">
        Complete your investor profile questionnaire to generate tailored scheme recommendations.
      </div>
    `;
    return;
  }

  list.forEach(s => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";
    card.style.border = "1px solid rgba(77, 141, 255, 0.3)";

    const riskBadgeClass = s.riskometer === "Very High" ? "badge-danger" : (s.riskometer === "Low to Moderate" ? "badge-active" : "badge-warning");

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 16px;">
        <div>
          <span style="font-size: 11px; color: var(--primary); font-weight: 700; text-transform: uppercase;">${escapeHtml(s.category)}</span>
          <h4 style="font-size: 14px; font-weight: 700; margin-top: 2px;">${escapeHtml(s.schemeName)}</h4>
        </div>
        <div style="text-align: right;">
          <span class="badge badge-active" style="font-size: 11px;">Match: ${s.matchScore}%</span>
        </div>
      </div>
      <div class="card-body" style="padding: 16px;">
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px;">
          <span style="color: var(--text-muted);">Verified AMFI NAV:</span>
          <strong>₹${s.nav != null ? s.nav.toFixed(2) : 'N/A'} <span style="font-size: 10px; color: var(--text-muted);">(${escapeHtml(s.navDate || '')})</span></strong>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px;">
          <span style="color: var(--text-muted);">3-Year CAGR / Return:</span>
          <strong style="color: var(--success);">${s.return3Y != null ? s.return3Y + '%' : (s.return1Y != null ? s.return1Y + '% (1Y)' : 'N/A')}</strong>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 6px;">
          <span style="color: var(--text-muted);">3-Year Annual Volatility:</span>
          <span>${s.volatility3Y != null ? s.volatility3Y + '%' : 'N/A'}</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 8px;">
          <span style="color: var(--text-muted);">Expense Ratio:</span>
          <span>${s.expenseRatioEstimated != null ? s.expenseRatioEstimated + '% p.a.' : 'N/A'}</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 12px;">
          <span style="color: var(--text-muted);">SEBI Riskometer:</span>
          <span class="badge ${riskBadgeClass}">${escapeHtml(s.riskometer || 'Moderate')}</span>
        </div>

        <div style="background: rgba(10, 16, 36, 0.7); border: 1px solid var(--border); border-radius: var(--radius-sm); padding: 10px; font-size: 11px; color: var(--text-secondary); margin-bottom: 12px; line-height: 1.4;">
          <strong>Why this matches:</strong> ${escapeHtml(s.suitabilityMatchReason || 'Aligned with your risk tolerance and horizon.')}
        </div>

        <div style="display: flex; justify-content: space-between; align-items: center;">
          <a href="${escapeHtml(s.officialAmcUrl || '#')}" target="_blank" rel="noopener noreferrer" style="font-size: 11px; color: var(--primary); text-decoration: none;">
            Official AMC Portal ↗
          </a>
          <button class="btn btn-secondary btn-sm" onclick="selectSchemeForSimulation('${s.schemeCode}', '${escapeHtml(s.schemeName)}')" style="font-size: 11px; padding: 4px 8px;">
            Simulate SIP
          </button>
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function renderAmfiSchemesTable(schemes) {
  const tbody = document.getElementById("amfi-schemes-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";

  if (schemes.length === 0) {
    tbody.innerHTML = `<tr><td colspan="11" style="text-align: center; color: var(--text-muted); padding: 30px;">No schemes found matching filter.</td></tr>`;
    return;
  }

  schemes.forEach(s => {
    const isChecked = selectedSchemesToCompare.has(s.schemeCode);
    const riskBadgeClass = s.riskometer === "Very High" ? "badge-danger" : (s.riskometer === "Low to Moderate" ? "badge-active" : "badge-warning");

    const tr = document.createElement("tr");
    tr.innerHTML = `
      <td>
        <input type="checkbox" value="${s.schemeCode}" ${isChecked ? 'checked' : ''} onchange="toggleSchemeCompare('${s.schemeCode}')">
      </td>
      <td>
        <strong>${escapeHtml(s.schemeName)}</strong>
        <div style="font-size: 11px; color: var(--text-muted);">Code: ${s.schemeCode} | ${escapeHtml(s.fundHouse)}</div>
      </td>
      <td><span class="badge" style="background: rgba(37, 99, 255, 0.15); color: #4D8DFF;">${escapeHtml(s.category)}</span></td>
      <td><strong>₹${s.nav != null ? s.nav.toFixed(2) : 'N/A'}</strong></td>
      <td style="font-size: 12px; color: var(--text-muted);">${escapeHtml(s.navDate || 'Delayed EOD')}</td>
      <td style="color: ${s.return1Y >= 0 ? 'var(--success)' : 'var(--danger)'}; font-weight: 600;">${s.return1Y != null ? s.return1Y + '%' : 'N/A'}</td>
      <td style="color: ${s.return3Y >= 0 ? 'var(--success)' : 'var(--danger)'}; font-weight: 600;">${s.return3Y != null ? s.return3Y + '%' : 'N/A'}</td>
      <td>${s.volatility3Y != null ? s.volatility3Y + '%' : 'N/A'}</td>
      <td>${s.expenseRatioEstimated != null ? s.expenseRatioEstimated + '%' : 'N/A'}</td>
      <td><span class="badge ${riskBadgeClass}">${escapeHtml(s.riskometer || 'Moderate')}</span></td>
      <td>
        <a href="${escapeHtml(s.officialAmcUrl || '#')}" target="_blank" rel="noopener noreferrer" class="btn btn-secondary btn-sm" style="padding: 2px 8px; font-size: 11px;">
          Portal ↗
        </a>
      </td>
    `;
    tbody.appendChild(tr);
  });
}

function filterSchemeCatalog() {
  const cat = document.getElementById("scheme-category-filter").value;
  if (!cat) {
    renderAmfiSchemesTable(cachedAmfiSchemes);
    return;
  }

  const filtered = cachedAmfiSchemes.filter(s => {
    if (cat === "EQUITY" || cat === "HYBRID" || cat === "DEBT") {
      return s.broadCategory === cat;
    }
    return (s.category && s.category.toLowerCase().includes(cat.toLowerCase()));
  });
  renderAmfiSchemesTable(filtered);
}

function toggleSchemeCompare(schemeCode) {
  if (selectedSchemesToCompare.has(schemeCode)) {
    selectedSchemesToCompare.delete(schemeCode);
  } else {
    if (selectedSchemesToCompare.size >= 4) {
      showToast("You can compare a maximum of 4 schemes simultaneously.", "warning");
      renderAmfiSchemesTable(cachedAmfiSchemes);
      return;
    }
    selectedSchemesToCompare.add(schemeCode);
  }

  const countSpan = document.getElementById("compare-count");
  const compareBtn = document.getElementById("btn-compare-schemes");
  if (countSpan) countSpan.textContent = selectedSchemesToCompare.size;
  if (compareBtn) compareBtn.disabled = selectedSchemesToCompare.size < 2;
}

function toggleSelectAllSchemes(checkbox) {
  if (checkbox.checked) {
    cachedAmfiSchemes.slice(0, 4).forEach(s => selectedSchemesToCompare.add(s.schemeCode));
  } else {
    selectedSchemesToCompare.clear();
  }
  const countSpan = document.getElementById("compare-count");
  const compareBtn = document.getElementById("btn-compare-schemes");
  if (countSpan) countSpan.textContent = selectedSchemesToCompare.size;
  if (compareBtn) compareBtn.disabled = selectedSchemesToCompare.size < 2;
  renderAmfiSchemesTable(cachedAmfiSchemes);
}

async function openSchemeComparisonModal() {
  if (selectedSchemesToCompare.size < 2) {
    showToast("Please select at least 2 schemes to view side-by-side comparison.", "warning");
    return;
  }

  try {
    const res = await apiRequest("/investments/research/compare", "POST", {
      schemeCodes: Array.from(selectedSchemesToCompare)
    });

    const verdictEl = document.getElementById("compare-modal-verdict");
    const container = document.getElementById("compare-table-container");
    const diffsEl = document.getElementById("compare-modal-diffs");

    if (verdictEl) verdictEl.textContent = res.categoryVerdict || "";

    if (container && res.schemes) {
      let tableHtml = `
        <table class="table" style="font-size: 12px;">
          <thead>
            <tr>
              <th>Comparison Metric</th>
              ${res.schemes.map(s => `<th>${escapeHtml(s.schemeName)}</th>`).join("")}
            </tr>
          </thead>
          <tbody>
            <tr>
              <td><strong>Category</strong></td>
              ${res.schemes.map(s => `<td>${escapeHtml(s.category)}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>Verified EOD NAV</strong></td>
              ${res.schemes.map(s => `<td><strong>₹${s.nav != null ? s.nav.toFixed(2) : 'N/A'}</strong> <span style="font-size: 10px; color: var(--text-muted);">(${escapeHtml(s.navDate || '')})</span></td>`).join("")}
            </tr>
            <tr>
              <td><strong>1-Year Return</strong></td>
              ${res.schemes.map(s => `<td style="color: ${s.return1Y >= 0 ? 'var(--success)' : 'var(--danger)'}; font-weight: 600;">${s.return1Y != null ? s.return1Y + '%' : 'N/A'}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>3-Year CAGR</strong></td>
              ${res.schemes.map(s => `<td style="color: ${s.return3Y >= 0 ? 'var(--success)' : 'var(--danger)'}; font-weight: 700;">${s.return3Y != null ? s.return3Y + '%' : 'N/A'}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>3-Year Volatility (Std Dev)</strong></td>
              ${res.schemes.map(s => `<td>${s.volatility3Y != null ? s.volatility3Y + '%' : 'N/A'}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>Direct Expense Ratio</strong></td>
              ${res.schemes.map(s => `<td>${s.expenseRatioEstimated != null ? s.expenseRatioEstimated + '% p.a.' : 'N/A'}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>Benchmark Index</strong></td>
              ${res.schemes.map(s => `<td>${escapeHtml(s.benchmark || 'N/A')}</td>`).join("")}
            </tr>
            <tr>
              <td><strong>SEBI Riskometer</strong></td>
              ${res.schemes.map(s => `<td><span class="badge ${s.riskometer === 'Very High' ? 'badge-danger' : (s.riskometer === 'Low to Moderate' ? 'badge-active' : 'badge-warning')}">${escapeHtml(s.riskometer || 'Moderate')}</span></td>`).join("")}
            </tr>
            <tr>
              <td><strong>Official Scheme Factsheet</strong></td>
              ${res.schemes.map(s => `<td><a href="${escapeHtml(s.officialAmcUrl || '#')}" target="_blank" rel="noopener noreferrer" style="color: var(--primary);">AMC Portal ↗</a></td>`).join("")}
            </tr>
          </tbody>
        </table>
      `;
      container.innerHTML = tableHtml;
    }

    if (diffsEl && res.keyDifferences) {
      diffsEl.innerHTML = `
        <h5 style="font-size: 13px; font-weight: 700; margin-bottom: 8px;">Key Analytic Insights:</h5>
        <ul style="font-size: 12px; color: var(--text-secondary); line-height: 1.6; padding-left: 20px;">
          ${res.keyDifferences.map(d => `<li>${escapeHtml(d)}</li>`).join("")}
        </ul>
      `;
    }

    openModal("scheme-compare-modal");
  } catch (err) {
    // Handled in api.js
  }
}

function selectSchemeForSimulation(schemeCode, schemeName) {
  const select = document.getElementById("sim-rate-select");
  const scheme = cachedAmfiSchemes.find(s => s.schemeCode === schemeCode);
  if (scheme && select) {
    if (scheme.broadCategory === "DEBT") select.value = "7.0";
    else if (scheme.broadCategory === "HYBRID") select.value = "10.0";
    else if (scheme.category && scheme.category.includes("Small")) select.value = "14.0";
    else select.value = "12.0";
  }
  updateSipSimulation();
  showToast(`Simulating compounding for: ${schemeName}`, "info");
  const simCard = document.getElementById("sim-amount-slider");
  if (simCard) simCard.scrollIntoView({ behavior: "smooth" });
}

function updateSipSimulation() {
  const p = parseFloat(document.getElementById("sim-amount-slider").value) || 1000;
  const years = parseInt(document.getElementById("sim-years-slider").value) || 5;
  const annualRate = parseFloat(document.getElementById("sim-rate-select").value) || 12.0;

  document.getElementById("sim-amount-display").textContent = formatCurrency(p);
  document.getElementById("sim-years-display").textContent = `${years} Year${years > 1 ? 's' : ''}`;

  const totalMonths = years * 12;
  const totalInvested = p * totalMonths;

  const r = (annualRate / 100.0) / 12.0;
  let futureValue;
  if (r > 0) {
    futureValue = p * ((Math.pow(1.0 + r, totalMonths) - 1.0) / r) * (1.0 + r);
  } else {
    futureValue = totalInvested;
  }
  const wealthGain = futureValue - totalInvested;

  document.getElementById("sim-total-invested").textContent = formatCurrency(totalInvested);
  document.getElementById("sim-future-value").textContent = formatCurrency(futureValue);
  document.getElementById("sim-wealth-gain").textContent = `+${formatCurrency(wealthGain)}`;
}

async function refreshAmfiDataLive() {
  try {
    showToast("Connecting to AMFI Official NAV download portal...", "info");
    const res = await apiRequest("/investments/research/refresh", "POST");
    showToast(`Refreshed AMFI data! ${res.totalSchemes} benchmark schemes updated.`, "success");
    loadSipResearchHub();
  } catch (err) {
    // Handled in api.js
  }
}

// ----------------------------------------------------------------
// Existing Registered SIP Schedule Handlers (Preserved 100%)
// ----------------------------------------------------------------
function renderSips(list) {
  const container = document.getElementById("sip-container");
  if (!container) return;
  container.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (list.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 40px; background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border-radius: var(--radius-lg); border: 1px solid var(--border);">
        <p style="font-size: 14px; margin-bottom: 8px;">No Systematic Investment Plans registered yet.</p>
        ${!isParent ? '<button class="btn btn-primary btn-sm" onclick="openSipModal()">Register First SIP</button>' : ''}
      </div>
    `;
    return;
  }

  list.forEach(sip => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";

    let deleteBtn = "";
    if (!isParent) {
      deleteBtn = `<button class="btn btn-secondary btn-sm" onclick="deleteSip(${sip.id})" style="padding: 3px 8px; font-size: 11px;">Remove</button>`;
    }

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <div>
          <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(sip.fundName)}</h4>
          <span style="font-size: 11px; color: var(--text-muted);">Due Day: ${sip.dueDayOfMonth}th of every month</span>
        </div>
        <div style="display: flex; align-items: center; gap: 8px;">
          <span class="badge badge-active">${sip.paymentStatus || 'ACTIVE'}</span>
          ${deleteBtn}
        </div>
      </div>
      <div class="card-body" style="padding: 18px;">
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;">
          <span>Monthly Installment:</span>
          <strong>${formatCurrency(sip.monthlyAmount)}</strong>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;">
          <span>Duration:</span>
          <span>${sip.durationMonths} Months (${(sip.durationMonths / 12).toFixed(1)} yrs)</span>
        </div>
        <div style="display: flex; justify-content: space-between; font-size: 13px; margin-bottom: 8px;">
          <span>Assumed Annual Return:</span>
          <span style="color: var(--primary); font-weight: 600;">${sip.assumedReturnRate}% p.a.</span>
        </div>
        <div style="border-top: 1px solid var(--border); padding-top: 10px; margin-top: 10px;">
          <div style="display: flex; justify-content: space-between; font-size: 12px; margin-bottom: 4px;">
            <span style="color: var(--text-muted);">Total Principal Invested:</span>
            <span>${formatCurrency(sip.totalInvested)}</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 14px; font-weight: 700; margin-bottom: 4px;">
            <span>Projected Maturity Value:</span>
            <span style="color: var(--success);">${formatCurrency(sip.estimatedFutureValue)}</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 12px; color: var(--success);">
            <span>Estimated Wealth Gain:</span>
            <span>+ ${formatCurrency(sip.estimatedWealthGain)}</span>
          </div>
        </div>
        <div style="margin-top: 12px; padding: 10px; background: rgba(10, 16, 36, 0.7); border: 1px solid var(--border); border-radius: var(--radius-sm); font-size: 11px; color: var(--text-secondary); line-height: 1.4;">
          * Projected with compound annuity formula: FV = P × [((1+i)ⁿ - 1)/i] × (1+i). Returns are market estimates and not guaranteed.
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

function openSipModal() {
  document.getElementById("sip-form").reset();
  document.getElementById("sip-id").value = "";
  document.getElementById("sip-date").value = new Date().toISOString().split("T")[0];
  document.getElementById("sip-day").value = 5;
  document.getElementById("sip-duration").value = 36;
  document.getElementById("sip-rate").value = 12.0;
  openModal("sip-modal");
}

async function handleSipSubmit(e) {
  e.preventDefault();
  const dto = {
    fundName: document.getElementById("sip-name").value.trim(),
    monthlyAmount: document.getElementById("sip-amount").value,
    dueDayOfMonth: document.getElementById("sip-day").value,
    startDate: document.getElementById("sip-date").value,
    durationMonths: document.getElementById("sip-duration").value,
    assumedReturnRate: document.getElementById("sip-rate").value
  };

  try {
    await apiRequest("/sips", "POST", dto);
    showToast("SIP plan registered successfully.", "success");
    closeModal("sip-modal");
    loadSips();
  } catch (err) {
    // Handled in api.js
  }
}

async function deleteSip(id) {
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to remove this SIP record?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/sips/${id}`, "DELETE");
      showToast("SIP record removed.", "success");
      closeModal("delete-modal");
      loadSips();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}
