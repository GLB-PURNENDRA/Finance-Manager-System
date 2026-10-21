// ==============================================================
// Nine Intelligent Financial Features Suite
// ==============================================================

let currentProposedAction = null;

// Sub-nav tab switching
document.querySelectorAll(".sub-nav-btn").forEach(btn => {
  btn.addEventListener("click", () => {
    document.querySelectorAll(".sub-nav-btn").forEach(b => b.classList.remove("active"));
    document.querySelectorAll(".intel-panel").forEach(p => p.classList.remove("active-panel"));

    btn.classList.add("active");
    const target = btn.getAttribute("data-intel");
    const panel = document.getElementById(target);
    if (panel) panel.classList.add("active-panel");
  });
});

async function loadIntelligenceSuite() {
  try {
    const report = await apiRequest("/intelligence/report");
    if (!report) return;

    // 1. Money Leaks
    renderMoneyLeaks(report.moneyLeaks || []);

    // 6. Anomalies
    renderAnomalies(report.anomalies || []);

    // 8. Goal Coach
    renderGoalCoach(report.coachSuggestions || []);

    // 9. Month-End Predictor
    renderPredictor(report.monthEndPrediction);

    // 4. Proposed Actions
    loadProposedActions();

    // AI Status & Chat History
    await loadAiStatus();
    await loadAiChatHistory();

  } catch (err) {
    console.error("Failed to load intelligence suite:", err);
  }
}

// 1. Money Leaks
function renderMoneyLeaks(leaks) {
  const container = document.getElementById("money-leaks-container");
  container.innerHTML = "";

  if (leaks.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 32px; background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border-radius: var(--radius-lg); border: 1px solid var(--border);">
        <p style="font-size: 14px; color: var(--success); font-weight: 600;">✓ No repetitive micro-spending leaks detected in the last 30 days.</p>
      </div>
    `;
    return;
  }

  leaks.forEach(l => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";

    let detailsList = "";
    if (l.recordDetails && l.recordDetails.length > 0) {
      detailsList = `<div style="margin-top: 10px; font-size: 11px; color: var(--text-secondary); background: rgba(10, 16, 36, 0.7); border: 1px solid var(--border); padding: 8px; border-radius: var(--radius-sm); max-height: 80px; overflow-y: auto;">` +
        l.recordDetails.map(d => `<div>• ${escapeHtml(d)}</div>`).join("") +
        `</div>`;
    }

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <div>
          <h4 style="font-size: 14px; font-weight: 700; text-transform: capitalize;">${escapeHtml(l.pattern)}</h4>
          <span style="font-size: 11px; color: var(--text-muted);">${escapeHtml(l.category)} • ${l.frequency} Occurrences</span>
        </div>
        <span class="badge" style="background: var(--danger-light); color: var(--danger); font-size: 12px; font-weight: 700;">
          ${formatCurrency(l.totalSpent)}
        </span>
      </div>
      <div class="card-body" style="padding: 18px;">
        <p style="font-size: 12px; color: var(--text-main); line-height: 1.4;">${escapeHtml(l.explanation)}</p>
        ${detailsList}
      </div>
    `;
    container.appendChild(card);
  });
}

// 2. What-If Simulator
async function runWhatIfSimulation() {
  const incomeAdj = document.getElementById("sim-income-adj").value;
  const foodCut = document.getElementById("sim-food-cut").value;
  const sipAdj = document.getElementById("sim-sip-adj").value;

  const payload = {
    monthlyIncomeAdjustment: Number(incomeAdj || 0),
    monthlySipAdjustment: Number(sipAdj || 0),
    categoryExpenseAdjustments: {
      "Food & Mess": Number(foodCut || 0)
    }
  };

  try {
    const res = await apiRequest("/intelligence/what-if", "POST", payload);
    const resultsCard = document.getElementById("sim-results-card");
    resultsCard.style.display = "block";

    document.getElementById("sim-explanation").textContent = res.explanation;
    document.getElementById("sim-base-net").textContent = formatCurrency(res.baselineNetCashFlow);
    document.getElementById("sim-projected-net").textContent = formatCurrency(res.simulatedNetCashFlow);
    document.getElementById("sim-annual-gain").textContent = "+" + formatCurrency(res.projectedAnnualSavingsImpact);

    showToast("What-If simulation evaluated successfully.", "success");
  } catch (err) {
    // Handled in api.js
  }
}

// 3. Ask Your Money AI — Fully Conversational Interface

function formatAiMarkdown(text) {
  if (!text) return "";
  let html = escapeHtml(text);

  // Headers
  html = html.replace(/^### (.*$)/gim, '<h3 style="margin: 6px 0 8px; color: var(--accent-blue-light); font-size: 14px; font-weight: 700;">$1</h3>');
  html = html.replace(/^## (.*$)/gim, '<h4 style="margin: 6px 0 8px; color: var(--text-primary); font-size: 14px; font-weight: 700;">$1</h4>');

  // Bold & Italic
  html = html.replace(/\*\*(.*?)\*\*/g, '<strong style="color: var(--text-primary); font-weight: 700;">$1</strong>');
  html = html.replace(/\*(.*?)\*/g, '<em style="color: var(--text-secondary);">$1</em>');

  // Bullet items
  html = html.replace(/^[•\-] (.*$)/gim, '<li style="margin-left: 18px; list-style-type: disc; margin-bottom: 4px;">$1</li>');

  // Numbered list items
  html = html.replace(/^(\d+)\. (.*$)/gim, '<li style="margin-left: 18px; list-style-type: decimal; margin-bottom: 4px;">$2</li>');

  // Double newline to paragraph spacer
  html = html.replace(/\n\n/g, '<div style="margin-bottom: 8px;"></div>');
  html = html.replace(/\n/g, '<br>');

  return html;
}

function handleChatInputKeydown(event) {
  if (event.key === "Enter" && !event.shiftKey) {
    event.preventDefault();
    submitChatMessage();
  }
}

function openAiConfigModal() {
  openModal("ai-config-modal");
}

async function saveAiConfig() {
  const keyInput = document.getElementById("ai-modal-api-key");
  const modelInput = document.getElementById("ai-modal-model-name");
  const apiKey = keyInput ? keyInput.value.trim() : "";
  const model = modelInput ? modelInput.value.trim() || "gemini-3.5-flash-lite" : "gemini-3.5-flash-lite";

  try {
    const res = await apiRequest("/intelligence/config", "POST", { apiKey, model });
    closeModal("ai-config-modal");
    if (keyInput) keyInput.value = "";
    await loadAiStatus();
    showToast(res.isRealLlmConfigured ? "Shyam's Finance AI activated!" : "Switched to Local Financial Knowledge mode.", "success");
  } catch (err) {
    // Handled in api.js
  }
}

async function loadAiStatus() {
  try {
    const status = await apiRequest("/intelligence/status");
    const badge = document.getElementById("ai-model-status-badge");
    if (badge && status) {
      if (status.isRealLlmConfigured) {
        badge.textContent = "Shyam's Finance AI";
        badge.style.background = "rgba(37, 214, 149, 0.15)";
        badge.style.borderColor = "rgba(37, 214, 149, 0.4)";
        badge.style.color = "var(--success)";
      } else {
        badge.textContent = "Deterministic Conversational AI (Demo Mode)";
        badge.style.background = "rgba(37, 99, 255, 0.15)";
        badge.style.borderColor = "var(--border-glow)";
        badge.style.color = "var(--accent-blue-light)";
      }
    }
  } catch (e) {
    // Silently continue
  }
}

async function loadAiChatHistory() {
  try {
    const history = await apiRequest("/intelligence/history");
    const container = document.getElementById("chat-messages");
    if (!container) return;

    if (history && history.length > 0) {
      container.innerHTML = "";
      history.forEach(m => {
        const sender = m.role === "user" ? "user" : "bot";
        const content = sender === "user" ? escapeHtml(m.content) : formatAiMarkdown(m.content);
        appendChatBubble(content, sender);
      });
    }
  } catch (e) {
    // Silently continue
  }
}

async function clearAiConversation() {
  try {
    await apiRequest("/intelligence/history", "DELETE");
    const container = document.getElementById("chat-messages");
    if (container) {
      container.innerHTML = `
        <div class="chat-bubble bot">
          Conversation context reset. Hello! I am Shyam's Finance AI, your Financial Intelligence Assistant. Ask me anything about your recorded income, expenses, category breakdowns, savings velocity, or month-over-month comparisons in English, Hindi, or Hinglish.
        </div>`;
    }
    showToast("Conversation session cleared.", "info");
  } catch (err) {
    // Handled in api.js
  }
}

async function submitChatMessage() {
  const input = document.getElementById("chat-prompt-input");
  const sendBtn = document.getElementById("btn-submit-chat");
  const typingIndicator = document.getElementById("chat-typing-indicator");
  const prompt = input.value.trim();
  if (!prompt) return;

  // Append user bubble
  appendChatBubble(escapeHtml(prompt), "user");
  input.value = "";
  input.style.height = "42px";

  // Show typing indicator & disable send
  if (typingIndicator) typingIndicator.style.display = "block";
  if (sendBtn) sendBtn.disabled = true;

  const typingText = document.getElementById("chat-typing-text");
  if (typingText) typingText.textContent = "Analyzing verified ledger records...";

  const t1 = setTimeout(() => {
    if (typingText) typingText.textContent = "Consulting Shyam's Finance AI...";
  }, 4000);

  const t2 = setTimeout(() => {
    if (typingText) typingText.textContent = "Synthesizing contextual financial advice...";
  }, 12000);

  const container = document.getElementById("chat-messages");
  if (container) container.scrollTop = container.scrollHeight;

  try {
    const res = await apiRequest("/intelligence/ask", "POST", { prompt });

    clearTimeout(t1);
    clearTimeout(t2);
    if (typingIndicator) typingIndicator.style.display = "none";
    if (sendBtn) sendBtn.disabled = false;
    if (typingText) typingText.textContent = "Analyzing verified ledger records...";

    let botHtml = formatAiMarkdown(res.answer);

    // Model & Mode indicator pill
    if (res.modelUsed) {
      let displayModel = res.modelUsed;
      if (res.realLlm || displayModel.toLowerCase().includes("gemini")) {
        displayModel = "Shyam's Finance AI";
      }
      botHtml += `<div style="margin-top: 10px; font-size: 10px; color: var(--text-muted); display: flex; align-items: center; gap: 6px;">
        <span style="display: inline-block; width: 6px; height: 6px; border-radius: 50%; background: ${res.realLlm ? 'var(--success)' : 'var(--accent-blue-light)'};"></span>
        Engine: ${escapeHtml(displayModel)}
      </div>`;
    }

    // Evidence Accordion
    if (res.evidenceRecords && res.evidenceRecords.length > 0) {
      botHtml += `<details style="margin-top: 8px; font-size: 11px; color: var(--text-secondary); border-top: 1px dashed rgba(77, 141, 255, 0.2); padding-top: 6px; cursor: pointer;">
        <summary style="outline: none; font-weight: 600; color: var(--text-muted);">View Grounded Evidence (${res.evidenceRecords.length} verified points)</summary>
        <div style="margin-top: 6px; line-height: 1.6;">`;
      res.evidenceRecords.forEach(e => {
        botHtml += `• ${escapeHtml(e)}<br>`;
      });
      botHtml += `</div></details>`;
    }

    appendChatBubble(botHtml, "bot");
  } catch (err) {
    clearTimeout(t1);
    clearTimeout(t2);
    if (typingIndicator) typingIndicator.style.display = "none";
    if (sendBtn) sendBtn.disabled = false;
    if (typingText) typingText.textContent = "Analyzing verified ledger records...";
    appendChatBubble("⚠️ Sorry, unable to compute financial insight for this query at the moment. Please verify backend ledger connectivity.", "bot");
  }
}

function sendQuickChat(text) {
  const input = document.getElementById("chat-prompt-input");
  input.value = text;
  submitChatMessage();
}

function appendChatBubble(content, sender) {
  const container = document.getElementById("chat-messages");
  const bubble = document.createElement("div");
  bubble.className = `chat-bubble ${sender}`;
  bubble.innerHTML = content;
  container.appendChild(bubble);
  container.scrollTop = container.scrollHeight;
}

// 4. Action-Taking Agent
async function loadProposedActions() {
  const container = document.getElementById("ai-actions-container");
  container.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";
  if (isParent) {
    container.innerHTML = `<div style="color: var(--text-muted); font-size: 13px;">Proposed actions are disabled in parent read-only mode.</div>`;
    return;
  }

  try {
    const proposals = await apiRequest("/intelligence/actions");
    if (!proposals || proposals.length === 0) {
      container.innerHTML = `
        <div style="background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 22px; text-align: center; color: var(--text-secondary); font-size: 13px;">
          ✓ All spending categories are currently within healthy parameters. No urgent budget actions needed.
        </div>
      `;
      return;
    }

    proposals.forEach(p => {
      const card = document.createElement("div");
      card.className = "card";
      card.style.marginBottom = "0";
      card.innerHTML = `
        <div class="card-header" style="padding: 14px 18px;">
          <h4 style="font-size: 14px; font-weight: 700; color: #5b21b6;">💡 ${escapeHtml(p.title)}</h4>
          <button class="btn btn-primary btn-sm" onclick="openConfirmActionModal(${JSON.stringify(p).replace(/"/g, '&quot;')})">
            Review & Apply
          </button>
        </div>
        <div class="card-body" style="padding: 14px 18px; font-size: 13px;">
          <p>${escapeHtml(p.description)}</p>
        </div>
      `;
      container.appendChild(card);
    });
  } catch (err) {
    console.error("Failed to load proposed actions:", err);
  }
}

function openConfirmActionModal(action) {
  currentProposedAction = action;
  document.getElementById("confirm-action-title").textContent = action.title;
  document.getElementById("confirm-action-desc").textContent = action.description;

  const detailsEl = document.getElementById("confirm-action-details");
  detailsEl.innerHTML = `
    <strong>Action Type:</strong> ${escapeHtml(action.actionType)}<br>
    <strong>Target Category:</strong> ${escapeHtml(action.payload.category || 'N/A')}<br>
    <strong>Proposed Limit:</strong> ${formatCurrency(action.payload.limitAmount || 0)}<br>
    <strong>Period:</strong> ${escapeHtml(action.payload.periodMonth || 'N/A')}
  `;

  document.getElementById("btn-execute-confirmed-action").onclick = executeConfirmedAction;
  openModal("confirm-action-modal");
}

async function executeConfirmedAction() {
  if (!currentProposedAction) return;

  try {
    const res = await apiRequest("/intelligence/actions/confirm", "POST", currentProposedAction);
    showToast(res.message, "success");
    closeModal("confirm-action-modal");
    currentProposedAction = null;
    loadProposedActions();
    loadBudgets();
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

// 6. Anomaly Radar
function renderAnomalies(anomalies) {
  const container = document.getElementById("anomalies-container");
  container.innerHTML = "";

  if (anomalies.length === 0) {
    container.innerHTML = `
      <div style="background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 24px; text-align: center; color: var(--text-secondary); font-size: 13px;">
        <span style="color: var(--success); font-weight: 600;">✓ Radar Clear:</span> No unusual surges or duplicate anomalies detected in the ledger.
      </div>
    `;
    return;
  }

  anomalies.forEach(a => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";

    const isDuplicate = a.type === "SUSPECTED_DUPLICATE";
    const badgeColor = isDuplicate ? "var(--warning)" : "var(--danger)";
    const badgeBg = isDuplicate ? "var(--warning-light)" : "var(--danger-light)";

    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <h4 style="font-size: 14px; font-weight: 700;">${escapeHtml(a.title)}</h4>
        <span class="badge" style="background: ${badgeBg}; color: ${badgeColor};">${a.type}</span>
      </div>
      <div class="card-body" style="padding: 14px 18px; font-size: 13px;">
        <p style="margin-bottom: 8px;">${escapeHtml(a.explanation)}</p>
        <span style="font-size: 11px; color: var(--text-muted);">Amount: ${formatCurrency(a.amount)} • Category: ${escapeHtml(a.category)} • Date: ${formatDate(a.transactionDate)}</span>
      </div>
    `;
    container.appendChild(card);
  });
}

// 8. Goal Coach
function renderGoalCoach(suggestions) {
  const container = document.getElementById("coach-container");
  container.innerHTML = "";

  if (suggestions.length === 0) {
    container.innerHTML = `
      <div style="grid-column: 1 / -1; text-align: center; color: var(--text-secondary); padding: 32px; background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border-radius: var(--radius-lg); border: 1px solid var(--border); font-size: 13px;">
        No active savings goals pending recommendations.
      </div>
    `;
    return;
  }

  suggestions.forEach(s => {
    const card = document.createElement("div");
    card.className = "card";
    card.style.marginBottom = "0";
    card.innerHTML = `
      <div class="card-header" style="padding: 14px 18px;">
        <h4 style="font-size: 14px; font-weight: 700;">🎯 ${escapeHtml(s.goalName)}</h4>
        <span class="badge badge-active">Feasibility Plan</span>
      </div>
      <div class="card-body" style="padding: 18px; font-size: 13px;">
        <div style="margin-bottom: 10px;">
          <span style="font-size: 11px; color: var(--text-secondary); text-transform: uppercase;">Recommended Monthly Allocation:</span>
          <div style="font-size: 20px; font-weight: 800; color: var(--primary);">${formatCurrency(s.recommendedMonthlySavings)}/mo</div>
        </div>
        <p style="margin-bottom: 10px; line-height: 1.4;">${escapeHtml(s.reasoning)}</p>
        <div style="background: rgba(10, 16, 36, 0.7); border-left: 3px solid var(--electric-blue); border: 1px solid var(--border); border-left-width: 3px; padding: 10px 14px; border-radius: var(--radius-sm); font-size: 12px; color: var(--text-secondary);">
          ${escapeHtml(s.suggestedBudgetTradeOff)}
        </div>
      </div>
    `;
    container.appendChild(card);
  });
}

// 9. Month-End Predictor
function renderPredictor(pred) {
  const container = document.getElementById("predictor-container");
  if (!pred) return;

  container.innerHTML = `
    <div style="background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 24px;">
      <div class="metrics-grid" style="margin-bottom: 18px;">
        <div>
          <span class="metric-title">Days Elapsed</span>
          <div style="font-size: 22px; font-weight: 800; color: var(--text-main);">${pred.daysElapsed} / ${pred.totalDaysInMonth} Days</div>
        </div>
        <div>
          <span class="metric-title">Daily Burn Rate</span>
          <div style="font-size: 22px; font-weight: 800; color: var(--electric-blue);">${formatCurrency(pred.dailyBurnRate)}/day</div>
        </div>
        <div>
          <span class="metric-title">Spent So Far</span>
          <div style="font-size: 22px; font-weight: 800; color: var(--danger);">${formatCurrency(pred.spentSoFar)}</div>
        </div>
        <div>
          <span class="metric-title">Projected Month-End Spend</span>
          <div style="font-size: 22px; font-weight: 800; color: var(--purple);">${formatCurrency(pred.estimatedMonthEndSpend)}</div>
        </div>
      </div>

      <div style="background: rgba(10, 16, 36, 0.7); border: 1px solid var(--border); border-radius: var(--radius-md); padding: 16px;">
        <h5 style="font-size: 13px; font-weight: 700; color: var(--electric-blue); margin-bottom: 4px;">Pace Confidence & Uncertainty Interval (±12%)</h5>
        <p style="font-size: 13px; color: var(--text-main); margin-bottom: 6px;">${escapeHtml(pred.confidenceMessage)}</p>
        <span style="font-size: 11px; color: var(--text-secondary);">
          Range Band: <strong style="color: var(--success);">${formatCurrency(pred.uncertaintyLow)}</strong> (Optimistic) to <strong style="color: var(--danger);">${formatCurrency(pred.uncertaintyHigh)}</strong> (Pessimistic).
          Calculation: Linear burn-rate projection extrapolated across remaining calendar days.
        </span>
      </div>
    </div>
  `;
}
