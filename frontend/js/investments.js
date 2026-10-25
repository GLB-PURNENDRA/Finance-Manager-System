// ==============================================================
// Investment Portfolio Management & Live Market Tracking
// ==============================================================

let currentPortfolioSummary = null;
let currentInvestmentList = [];
let lastFetchedQuote = null;
let symbolSearchDebounceTimer = null;

const ASSET_COLORS = {
  INDIAN_STOCKS: "#D4AF37",
  US_STOCKS: "#4D8DFF",
  ETFS: "#9C6BFF",
  MUTUAL_FUNDS: "#00D09C",
  GOLD: "#F5D77A",
  CRYPTO: "#FF9F43",
  BONDS: "#6C757D",
  FIXED_DEPOSIT: "#20C997",
  OTHER: "#A5A5AD"
};

const ASSET_LABELS = {
  INDIAN_STOCKS: "Indian Stocks",
  US_STOCKS: "US Stocks",
  ETFS: "ETFs",
  MUTUAL_FUNDS: "Mutual Funds",
  GOLD: "Gold",
  CRYPTO: "Crypto",
  BONDS: "Bonds",
  FIXED_DEPOSIT: "Fixed Deposit",
  OTHER: "Other Assets"
};

/**
 * Loads portfolio summary and holdings from backend API.
 */
async function loadInvestments() {
  try {
    const summary = await apiRequest("/investments/summary");
    currentPortfolioSummary = summary || {};
    currentInvestmentList = (summary && summary.investments) ? summary.investments : [];

    renderPortfolioKpis(currentPortfolioSummary);
    renderAssetAllocation(currentPortfolioSummary);
    renderInvestments(currentInvestmentList);

    const syncEl = document.getElementById("inv-last-sync-text");
    if (syncEl) {
      const now = new Date();
      syncEl.textContent = `Synced: ${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`;
    }
  } catch (err) {
    console.error("Failed to load investment portfolio:", err);
    // Fallback to basic list if summary endpoint is busy
    try {
      const list = await apiRequest("/investments");
      currentInvestmentList = list || [];
      renderInvestments(currentInvestmentList);
    } catch (e) {
      console.error("Fallback load failed:", e);
    }
  }
}

/**
 * Refreshes live market prices across all held assets.
 */
async function refreshPortfolioLive() {
  const refreshBtn = document.getElementById("btn-refresh-inv");
  const refreshIcon = document.getElementById("inv-refresh-icon");

  try {
    if (refreshBtn) refreshBtn.disabled = true;
    if (refreshIcon) refreshIcon.classList.add("spin-animation");

    showToast("Connecting to live financial market APIs...", "info");
    const updatedSummary = await apiRequest("/investments/refresh-prices", "POST");
    
    currentPortfolioSummary = updatedSummary || {};
    currentInvestmentList = (updatedSummary && updatedSummary.investments) ? updatedSummary.investments : [];

    renderPortfolioKpis(currentPortfolioSummary);
    renderAssetAllocation(currentPortfolioSummary);
    renderInvestments(currentInvestmentList);

    const syncEl = document.getElementById("inv-last-sync-text");
    if (syncEl) {
      const now = new Date();
      syncEl.textContent = `Synced: ${now.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}`;
    }

    showToast("Portfolio live prices updated successfully.", "success");
  } catch (err) {
    console.error("Failed to refresh live prices:", err);
    showToast("Price refresh failed: " + (err.message || "Network error"), "error");
  } finally {
    if (refreshBtn) refreshBtn.disabled = false;
    if (refreshIcon) refreshIcon.classList.remove("spin-animation");
  }
}

/**
 * Renders portfolio header KPI metric cards.
 */
function renderPortfolioKpis(summary) {
  const currentValEl = document.getElementById("inv-kpi-current-val");
  const investedEl = document.getElementById("inv-kpi-invested");
  const pnlEl = document.getElementById("inv-kpi-pnl");
  const pnlPctEl = document.getElementById("inv-kpi-pnl-pct");
  const dayChangeEl = document.getElementById("inv-kpi-daychange");
  const countEl = document.getElementById("inv-kpi-asset-count");

  const totalCurrent = Number(summary.totalCurrentValue || 0);
  const totalInvested = Number(summary.totalInvested || 0);
  const totalPnl = Number(summary.totalProfitLoss || 0);
  const pnlPct = Number(summary.totalProfitLossPercentage || 0);
  const dayChange = Number(summary.todayGainLoss || 0);
  const count = summary.assetCount || 0;

  if (currentValEl) currentValEl.textContent = formatCurrency(totalCurrent);
  if (investedEl) investedEl.textContent = formatCurrency(totalInvested);
  if (countEl) countEl.textContent = `${count} Active Holding${count === 1 ? '' : 's'}`;

  const isProfit = totalPnl >= 0;
  if (pnlEl) {
    pnlEl.textContent = `${isProfit ? '+' : ''}${formatCurrency(totalPnl)}`;
    pnlEl.style.color = isProfit ? "var(--success)" : "var(--danger)";
  }

  if (pnlPctEl) {
    pnlPctEl.textContent = `${isProfit ? '+' : ''}${pnlPct.toFixed(2)}%`;
    pnlPctEl.style.background = isProfit ? "rgba(0, 208, 156, 0.15)" : "rgba(255, 90, 95, 0.15)";
    pnlPctEl.style.color = isProfit ? "var(--success)" : "var(--danger)";
    pnlPctEl.style.border = `1px solid ${isProfit ? 'rgba(0, 208, 156, 0.3)' : 'rgba(255, 90, 95, 0.3)'}`;
  }

  const isDayGain = dayChange >= 0;
  if (dayChangeEl) {
    dayChangeEl.textContent = `${isDayGain ? '+' : ''}${formatCurrency(dayChange)}`;
    dayChangeEl.style.color = isDayGain ? "var(--success)" : "var(--danger)";
  }
}

/**
 * Renders the visual asset allocation bar and interactive legend.
 */
function renderAssetAllocation(summary) {
  const barEl = document.getElementById("inv-allocation-bar");
  const legendEl = document.getElementById("inv-allocation-legend");

  if (!barEl || !legendEl) return;

  barEl.innerHTML = "";
  legendEl.innerHTML = "";

  const allocation = summary.allocationByType || {};
  const values = summary.valueByType || {};
  const totalVal = Number(summary.totalCurrentValue || 0);

  const keys = Object.keys(allocation);

  if (keys.length === 0 || totalVal <= 0) {
    barEl.innerHTML = `<div style="width: 100%; height: 100%; background: rgba(255, 255, 255, 0.08); text-align: center; font-size: 10px; color: var(--text-muted); line-height: 14px;">No allocation data</div>`;
    legendEl.innerHTML = `<div style="grid-column: 1 / -1; font-size: 12px; color: var(--text-muted); text-align: center; padding: 10px;">Add investment assets to view distribution breakdown</div>`;
    return;
  }

  keys.forEach(key => {
    const pct = Number(allocation[key] || 0);
    const val = Number(values[key] || 0);
    const color = ASSET_COLORS[key] || ASSET_COLORS.OTHER;
    const label = ASSET_LABELS[key] || key;

    if (pct > 0) {
      // Bar segment
      const segment = document.createElement("div");
      segment.className = "allocation-segment";
      segment.style.width = `${pct}%`;
      segment.style.backgroundColor = color;
      segment.title = `${label}: ${pct}% (${formatCurrency(val)})`;
      barEl.appendChild(segment);

      // Legend item
      const legItem = document.createElement("div");
      legItem.className = "allocation-legend-item";
      legItem.innerHTML = `
        <div class="legend-color-dot" style="background-color: ${color}; box-shadow: 0 0 6px ${color}88;"></div>
        <div class="legend-text">
          <div class="legend-title">${escapeHtml(label)}</div>
          <div class="legend-meta"><strong>${pct.toFixed(1)}%</strong> • ${formatCurrency(val)}</div>
        </div>
      `;
      legendEl.appendChild(legItem);
    }
  });
}

/**
 * Filters the holdings table dynamically based on category dropdown and search box.
 */
function filterInvestmentsTable() {
  const typeFilter = document.getElementById("inv-filter-type").value;
  const searchFilter = (document.getElementById("inv-filter-search").value || "").trim().toLowerCase();

  const filtered = currentInvestmentList.filter(inv => {
    const matchesType = (typeFilter === "ALL" || (inv.assetType || "").toUpperCase() === typeFilter);
    const matchesSearch = !searchFilter ||
      (inv.assetName && inv.assetName.toLowerCase().includes(searchFilter)) ||
      (inv.symbol && inv.symbol.toLowerCase().includes(searchFilter)) ||
      (inv.notes && inv.notes.toLowerCase().includes(searchFilter));
    return matchesType && matchesSearch;
  });

  renderInvestments(filtered);
}

/**
 * Renders the holdings table body with real-time market data indicators.
 */
function renderInvestments(list) {
  const tbody = document.getElementById("investments-tbody");
  if (!tbody) return;
  tbody.innerHTML = "";

  const isParent = currentUser && currentUser.role === "PARENT";

  if (!list || list.length === 0) {
    tbody.innerHTML = `
      <tr>
        <td colspan="${isParent ? 8 : 9}" style="text-align: center; color: var(--text-muted); padding: 40px 16px;">
          <div style="font-size: 28px; margin-bottom: 8px;">💼</div>
          <div style="font-weight: 600; color: #FFFFFF; margin-bottom: 4px;">No investment holdings recorded</div>
          <div style="font-size: 12px; margin-bottom: 12px;">Add stocks, ETFs, mutual funds, gold or crypto to enable automated live market tracking</div>
          ${!isParent ? `<button class="btn btn-primary btn-sm" onclick="openInvestmentModal()">+ Add Your First Investment</button>` : ''}
        </td>
      </tr>
    `;
    return;
  }

  list.forEach(inv => {
    const tr = document.createElement("tr");
    const isProfit = Number(inv.profitLoss || 0) >= 0;
    const color = ASSET_COLORS[inv.assetType] || ASSET_COLORS.OTHER;
    const typeLabel = ASSET_LABELS[inv.assetType] || inv.assetType;

    // Day change indicator
    let dayChangeHtml = "";
    if (inv.dayChange != null && inv.dayChange !== undefined && Number(inv.dayChange) !== 0) {
      const isUp = Number(inv.dayChange) > 0;
      dayChangeHtml = `
        <div style="font-size: 10.5px; font-weight: 600; color: ${isUp ? 'var(--success)' : 'var(--danger)'}; margin-top: 2px;">
          ${isUp ? '▲ +' : '▼ '}${formatCurrency(inv.dayChange, inv.currency)} (${inv.dayChangePercentage}%)
        </div>
      `;
    }

    let actionsHtml = "";
    if (!isParent) {
      actionsHtml = `
        <td class="student-only" style="white-space: nowrap;">
          <button class="btn btn-secondary btn-sm" onclick="openEditInvestmentModal(${JSON.stringify(inv).replace(/"/g, '&quot;')})" title="Edit investment">
            Edit
          </button>
          <button class="btn btn-danger btn-sm" onclick="deleteInvestment(${inv.id})" title="Delete investment" style="margin-left: 4px;">
            Delete
          </button>
        </td>
      `;
    }

    const curr = inv.currency || "INR";

    tr.innerHTML = `
      <td>
        <div style="font-weight: 700; color: #FFFFFF;">${escapeHtml(inv.assetName)}</div>
        ${inv.symbol ? `<div style="font-size: 11px; color: var(--primary-bright); font-family: monospace; letter-spacing: 0.5px;">${escapeHtml(inv.symbol)}</div>` : ''}
      </td>
      <td>
        <span class="badge" style="background: ${color}1A; color: ${color}; border: 1px solid ${color}44; font-size: 11px;">
          ${escapeHtml(typeLabel)}
        </span>
      </td>
      <td style="font-size: 12px; color: var(--text-muted);">${formatQuantity(inv.quantity)}</td>
      <td style="font-size: 12px;">${formatCurrency(inv.purchasePrice, curr)}</td>
      <td>
        <div style="font-weight: 700; color: var(--text-gold);">${formatCurrency(inv.currentPrice, curr)}</div>
        ${dayChangeHtml}
      </td>
      <td style="font-size: 12.5px;">${formatCurrency(inv.investedAmount, curr)}</td>
      <td style="font-weight: 700; color: #FFFFFF; font-size: 13px;">${formatCurrency(inv.currentValue, curr)}</td>
      <td>
        <div style="font-weight: 700; color: ${isProfit ? 'var(--success)' : 'var(--danger)'};">
          ${isProfit ? '+' : ''}${formatCurrency(inv.profitLoss, curr)}
        </div>
        <span class="badge" style="background: ${isProfit ? 'var(--success-light)' : 'var(--danger-light)'}; color: ${isProfit ? 'var(--success)' : 'var(--danger)'}; font-size: 10.5px; margin-top: 2px;">
          ${isProfit ? '+' : ''}${inv.profitLossPercentage}%
        </span>
      </td>
      ${actionsHtml}
    `;

    tbody.appendChild(tr);
  });
}

function formatQuantity(qty) {
  if (qty == null) return "1";
  const num = Number(qty);
  if (num % 1 === 0) return num.toString();
  return num.toFixed(4).replace(/\.?0+$/, "");
}

/**
 * Handles adaptive form changes based on chosen asset category.
 */
function handleAssetTypeChange() {
  const type = document.getElementById("inv-type").value;
  const symbolLbl = document.getElementById("lbl-inv-symbol");
  const symbolInput = document.getElementById("inv-symbol");
  const hintSymbol = document.getElementById("hint-inv-symbol");
  const qtyLbl = document.getElementById("lbl-inv-qty");
  const buyPriceLbl = document.getElementById("lbl-inv-buy-price");
  const chargesLbl = document.getElementById("lbl-inv-charges");
  const currencySel = document.getElementById("inv-currency");

  hideAutocompleteDropdown();

  switch (type) {
    case "INDIAN_STOCKS":
      symbolLbl.textContent = "Stock Ticker (NSE / BSE)";
      symbolInput.placeholder = "e.g. RELIANCE.NS, TCS.NS, INFY.NS";
      hintSymbol.textContent = "Enter NSE ticker symbol (e.g. RELIANCE.NS) for live market prices";
      qtyLbl.textContent = "Quantity (Shares)";
      buyPriceLbl.textContent = "Purchase Price Per Share (₹)";
      chargesLbl.textContent = "Brokerage & Charges (STT, Stamp Duty) (₹)";
      currencySel.value = "INR";
      break;
    case "US_STOCKS":
      symbolLbl.textContent = "US Stock Ticker (NASDAQ / NYSE)";
      symbolInput.placeholder = "e.g. AAPL, MSFT, NVDA, GOOGL, TSLA";
      hintSymbol.textContent = "Enter US ticker for real-time market data in USD";
      qtyLbl.textContent = "Quantity (Shares)";
      buyPriceLbl.textContent = "Purchase Price Per Share ($)";
      chargesLbl.textContent = "Brokerage / Wire Charges ($)";
      currencySel.value = "USD";
      break;
    case "ETFS":
      symbolLbl.textContent = "ETF Symbol / Ticker";
      symbolInput.placeholder = "e.g. NIFTYBEES.NS, BANKBEES.NS, SPY, QQQ";
      hintSymbol.textContent = "Enter ETF ticker for real-time index tracking";
      qtyLbl.textContent = "Quantity (Units)";
      buyPriceLbl.textContent = "Purchase Price Per Unit";
      chargesLbl.textContent = "Brokerage / Charges";
      break;
    case "MUTUAL_FUNDS":
      symbolLbl.textContent = "AMFI Scheme Code";
      symbolInput.placeholder = "e.g. 120716, 122639, 119598";
      hintSymbol.textContent = "Enter 6-digit AMFI scheme code for official daily NAV";
      qtyLbl.textContent = "Units Allotted";
      buyPriceLbl.textContent = "Purchase NAV (Net Asset Value) (₹)";
      chargesLbl.textContent = "Stamp Duty / Transaction Fees (₹)";
      currencySel.value = "INR";
      break;
    case "GOLD":
      symbolLbl.textContent = "Gold Asset Symbol";
      symbolInput.placeholder = "e.g. GOLDBEES.NS, SILVERBEES.NS";
      hintSymbol.textContent = "GOLDBEES.NS tracks 1 gram of physical gold on NSE";
      qtyLbl.textContent = "Quantity (Grams / Units)";
      buyPriceLbl.textContent = "Purchase Price Per Gram (₹)";
      chargesLbl.textContent = "Making / GST / Platform Charges (₹)";
      currencySel.value = "INR";
      break;
    case "CRYPTO":
      symbolLbl.textContent = "Crypto Ticker Symbol";
      symbolInput.placeholder = "e.g. BTC, ETH, SOL, BNB";
      hintSymbol.textContent = "Enter coin ticker to auto-fetch spot prices via CoinGecko";
      qtyLbl.textContent = "Quantity (Coins / Decimal Units)";
      buyPriceLbl.textContent = "Buy Price Per Unit";
      chargesLbl.textContent = "Network / Gas / Exchange Fees";
      currencySel.value = "INR";
      break;
    case "BONDS":
    case "FIXED_DEPOSIT":
      symbolLbl.textContent = "Bond / Account Identifier (Optional)";
      symbolInput.placeholder = "e.g. RBI-BOND-2029, HDFC-FD-8921";
      hintSymbol.textContent = "Optional reference code or deposit folio";
      qtyLbl.textContent = "Units / Certificates (default 1)";
      buyPriceLbl.textContent = "Principal / Face Value Per Unit (₹)";
      chargesLbl.textContent = "Processing Fees (₹)";
      currencySel.value = "INR";
      break;
    default:
      symbolLbl.textContent = "Symbol / Identifier";
      symbolInput.placeholder = "e.g. Ticker or Scheme code";
      hintSymbol.textContent = "Asset identifier for market price synchronization";
      qtyLbl.textContent = "Quantity";
      buyPriceLbl.textContent = "Purchase Price";
      chargesLbl.textContent = "Charges";
      currencySel.value = "INR";
  }
}

/**
 * Live search suggestions for symbols.
 */
function handleSymbolInput(val) {
  clearTimeout(symbolSearchDebounceTimer);
  const dropdown = document.getElementById("symbol-autocomplete-dropdown");
  const query = (val || "").trim();

  if (query.length < 1) {
    dropdown.style.display = "none";
    dropdown.innerHTML = "";
    return;
  }

  symbolSearchDebounceTimer = setTimeout(async () => {
    try {
      const assetType = document.getElementById("inv-type").value;
      const results = await apiRequest(`/investments/search?query=${encodeURIComponent(query)}&assetType=${encodeURIComponent(assetType)}`);
      
      if (results && results.length > 0) {
        dropdown.innerHTML = "";
        results.forEach(item => {
          const div = document.createElement("div");
          div.className = "autocomplete-item";
          div.innerHTML = `
            <div class="autocomplete-item-left">
              <div class="autocomplete-symbol">
                <span>${escapeHtml(item.symbol)}</span>
                <span class="badge" style="font-size: 9.5px; padding: 2px 6px;">${escapeHtml(item.exchange || item.assetType)}</span>
              </div>
              <div class="autocomplete-name">${escapeHtml(item.name)}</div>
            </div>
            <div class="autocomplete-item-right">
              ${item.suggestedPrice ? `<div class="autocomplete-price">${formatCurrency(item.suggestedPrice, item.currency)}</div>` : ''}
              <div class="autocomplete-exchange">${escapeHtml(item.currency || 'INR')}</div>
            </div>
          `;
          div.onclick = () => selectSymbolSuggestion(item);
          dropdown.appendChild(div);
        });
        dropdown.style.display = "block";
      } else {
        dropdown.style.display = "none";
      }
    } catch (err) {
      console.warn("Autocomplete error:", err);
      dropdown.style.display = "none";
    }
  }, 220);
}

function hideAutocompleteDropdown() {
  const dropdown = document.getElementById("symbol-autocomplete-dropdown");
  if (dropdown) dropdown.style.display = "none";
}

/**
 * Handles selection of a symbol from autocomplete dropdown.
 */
function selectSymbolSuggestion(item) {
  document.getElementById("inv-symbol").value = item.symbol;
  if (!document.getElementById("inv-name").value || document.getElementById("inv-name").value.trim() === "") {
    document.getElementById("inv-name").value = item.name;
  }
  if (item.currency) {
    document.getElementById("inv-currency").value = item.currency;
  }
  hideAutocompleteDropdown();
  fetchLiveQuoteForSymbol();
}

/**
 * Fetches real-time market quote preview for the entered symbol.
 */
async function fetchLiveQuoteForSymbol() {
  const symbol = (document.getElementById("inv-symbol").value || "").trim();
  const assetType = document.getElementById("inv-type").value;
  const quoteBtn = document.getElementById("btn-fetch-quote");
  const callout = document.getElementById("inv-live-price-callout");

  if (!symbol) {
    showToast("Please enter a stock ticker or scheme code first.", "warning");
    return;
  }

  try {
    if (quoteBtn) {
      quoteBtn.disabled = true;
      quoteBtn.textContent = "⏳...";
    }

    const quote = await apiRequest(`/investments/quote?symbol=${encodeURIComponent(symbol)}&assetType=${encodeURIComponent(assetType)}`);

    if (quote && quote.price) {
      lastFetchedQuote = quote;

      // Populate callout
      document.getElementById("callout-symbol-text").textContent = quote.symbol;
      document.getElementById("callout-price-text").textContent = formatCurrency(quote.price, quote.currency);

      const changeBadge = document.getElementById("callout-change-badge");
      const isUp = Number(quote.dayChange || 0) >= 0;
      changeBadge.textContent = `${isUp ? '+' : ''}${quote.dayChangePercentage}%`;
      changeBadge.style.background = isUp ? "rgba(0, 208, 156, 0.15)" : "rgba(255, 90, 95, 0.15)";
      changeBadge.style.color = isUp ? "var(--success)" : "var(--danger)";

      document.getElementById("callout-source-text").textContent = `Source: ${quote.source || 'Financial API'} • ${quote.name || symbol}`;
      callout.style.display = "block";

      // Populate asset name if empty
      const nameInput = document.getElementById("inv-name");
      if (!nameInput.value || nameInput.value.trim() === "" || nameInput.value === symbol) {
        nameInput.value = quote.name || symbol;
      }

      // Update current market price input
      document.getElementById("inv-current-price").value = quote.price;

      // If buy price is 0 or empty, suggest setting it
      const buyPriceInput = document.getElementById("inv-buy-price");
      if (!buyPriceInput.value || Number(buyPriceInput.value) === 0) {
        buyPriceInput.value = quote.price;
      }

      if (quote.currency) {
        document.getElementById("inv-currency").value = quote.currency;
      }

      recalcInvestedAndCurrent();
      showToast(`Live price synced: ${formatCurrency(quote.price, quote.currency)}`, "success");
    } else {
      showToast("Symbol not recognized by financial market provider. You can enter prices manually.", "warning");
      callout.style.display = "none";
    }
  } catch (err) {
    console.warn("Quote fetch failed:", err);
    showToast("Live quote lookup unavailable for this symbol. Manual valuation enabled.", "info");
    callout.style.display = "none";
  } finally {
    if (quoteBtn) {
      quoteBtn.disabled = false;
      quoteBtn.textContent = "⚡ Quote";
    }
  }
}

/**
 * Copies the live quote price to the Purchase Price field.
 */
function applyQuoteToPurchasePrice() {
  if (lastFetchedQuote && lastFetchedQuote.price) {
    document.getElementById("inv-buy-price").value = lastFetchedQuote.price;
    recalcInvestedAndCurrent();
    showToast("Applied live market price as purchase price.", "info");
  }
}

/**
 * Automatically recalculates Invested Capital and Current Valuation.
 */
function recalcInvestedAndCurrent() {
  const qty = parseFloat(document.getElementById("inv-qty").value) || 0;
  const buyPrice = parseFloat(document.getElementById("inv-buy-price").value) || 0;
  const charges = parseFloat(document.getElementById("inv-charges").value) || 0;

  // Invested Capital = (Qty * BuyPrice) + Charges
  const invested = (qty * buyPrice) + charges;
  document.getElementById("inv-invested").value = invested > 0 ? invested.toFixed(2) : "0.00";

  // Current Valuation = Qty * CurrentPrice (fallback to buyPrice)
  const currentPriceInput = document.getElementById("inv-current-price");
  let currentPrice = parseFloat(currentPriceInput.value);
  if (isNaN(currentPrice) || currentPrice <= 0) {
    currentPrice = buyPrice;
  }

  const currentVal = qty * currentPrice;
  document.getElementById("inv-current").value = currentVal >= 0 ? currentVal.toFixed(2) : "0.00";
}

/**
 * Opens Add Investment Modal with clean defaults.
 */
function openInvestmentModal() {
  document.getElementById("inv-form").reset();
  document.getElementById("inv-id").value = "";
  document.getElementById("inv-modal-title").textContent = "Add Investment Asset";
  document.getElementById("btn-save-inv").textContent = "Save Investment";

  const callout = document.getElementById("inv-live-price-callout");
  if (callout) callout.style.display = "none";
  hideAutocompleteDropdown();
  lastFetchedQuote = null;

  // Set default date to today
  const dateInput = document.getElementById("inv-date");
  if (dateInput) {
    const today = new Date().toISOString().split("T")[0];
    dateInput.value = today;
  }

  document.getElementById("inv-qty").value = "1";
  document.getElementById("inv-charges").value = "0.00";

  handleAssetTypeChange();
  openModal("investment-modal");
}

/**
 * Opens Edit Investment Modal populated with existing data.
 */
function openEditInvestmentModal(inv) {
  document.getElementById("inv-id").value = inv.id;
  document.getElementById("inv-modal-title").textContent = "Update Investment Valuation";
  document.getElementById("btn-save-inv").textContent = "Update Investment";

  document.getElementById("inv-type").value = inv.assetType || "INDIAN_STOCKS";
  document.getElementById("inv-name").value = inv.assetName || "";
  document.getElementById("inv-symbol").value = inv.symbol || "";
  document.getElementById("inv-qty").value = inv.quantity != null ? inv.quantity : "1";
  document.getElementById("inv-buy-price").value = inv.purchasePrice != null ? inv.purchasePrice : "0.00";
  document.getElementById("inv-charges").value = inv.brokerageCharges != null ? inv.brokerageCharges : "0.00";
  document.getElementById("inv-invested").value = inv.investedAmount != null ? inv.investedAmount : "0.00";
  document.getElementById("inv-current-price").value = inv.currentPrice != null ? inv.currentPrice : "";
  document.getElementById("inv-current").value = inv.currentValue != null ? inv.currentValue : "0.00";
  document.getElementById("inv-currency").value = inv.currency || "INR";
  document.getElementById("inv-date").value = inv.investmentDate || new Date().toISOString().split("T")[0];
  document.getElementById("inv-notes").value = inv.notes || "";

  handleAssetTypeChange();

  const callout = document.getElementById("inv-live-price-callout");
  if (callout) callout.style.display = "none";
  hideAutocompleteDropdown();

  // If asset has a symbol, fetch quote preview
  if (inv.symbol) {
    fetchLiveQuoteForSymbol();
  }

  openModal("investment-modal");
}

/**
 * Handles Add/Edit Investment form submission.
 */
async function handleInvestmentSubmit(e) {
  e.preventDefault();
  const id = document.getElementById("inv-id").value;

  const qty = parseFloat(document.getElementById("inv-qty").value) || 1;
  const buyPrice = parseFloat(document.getElementById("inv-buy-price").value) || 0;
  const charges = parseFloat(document.getElementById("inv-charges").value) || 0;
  let invested = parseFloat(document.getElementById("inv-invested").value);
  if (isNaN(invested) || invested <= 0) {
    invested = (qty * buyPrice) + charges;
  }

  let currentPrice = parseFloat(document.getElementById("inv-current-price").value);
  if (isNaN(currentPrice) || currentPrice <= 0) {
    currentPrice = buyPrice;
  }

  let currentVal = parseFloat(document.getElementById("inv-current").value);
  if (isNaN(currentVal) || currentVal <= 0) {
    currentVal = qty * currentPrice;
  }

  const dto = {
    assetType: document.getElementById("inv-type").value,
    assetName: document.getElementById("inv-name").value.trim(),
    symbol: (document.getElementById("inv-symbol").value || "").trim(),
    quantity: qty,
    purchasePrice: buyPrice,
    brokerageCharges: charges,
    investedAmount: invested,
    currentPrice: currentPrice,
    currentValue: currentVal,
    currency: document.getElementById("inv-currency").value,
    investmentDate: document.getElementById("inv-date").value || null,
    notes: (document.getElementById("inv-notes").value || "").trim()
  };

  try {
    if (id) {
      await apiRequest(`/investments/${id}`, "PUT", dto);
      showToast("Investment valuation updated.", "success");
    } else {
      await apiRequest("/investments", "POST", dto);
      showToast("Investment asset registered with live pricing.", "success");
    }
    closeModal("investment-modal");
    loadInvestments();
    if (typeof loadDashboard === "function") loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

/**
 * Deletes an investment asset after confirmation.
 */
async function deleteInvestment(id) {
  document.getElementById("delete-prompt-message").textContent = "Are you sure you want to remove this investment asset from your portfolio?";
  const confirmBtn = document.getElementById("btn-confirm-delete");
  confirmBtn.onclick = async () => {
    try {
      await apiRequest(`/investments/${id}`, "DELETE");
      showToast("Investment removed.", "success");
      closeModal("delete-modal");
      loadInvestments();
      if (typeof loadDashboard === "function") loadDashboard();
    } catch (err) {
      // Handled in api.js
    }
  };
  openModal("delete-modal");
}

// Close autocomplete dropdown on outside click
document.addEventListener("click", function(e) {
  const dropdown = document.getElementById("symbol-autocomplete-dropdown");
  const symbolInput = document.getElementById("inv-symbol");
  if (dropdown && symbolInput && !dropdown.contains(e.target) && e.target !== symbolInput) {
    dropdown.style.display = "none";
  }
});
