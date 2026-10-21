// ==============================================================
// Family Linking & Access Governance
// ==============================================================

async function loadFamilyLinks() {
  const isParent = currentUser && currentUser.role === "PARENT";
  const studentPanel = document.getElementById("student-family-panel");
  const parentPanel = document.getElementById("parent-family-panel");

  if (isParent) {
    studentPanel.style.display = "none";
    parentPanel.style.display = "block";
    loadParentLinkStatus();
  } else {
    studentPanel.style.display = "block";
    parentPanel.style.display = "none";
    loadStudentLinkRequests();
  }
}

async function loadStudentLinkRequests() {
  // Set invitation code
  const codeInput = document.getElementById("student-invite-code-input");
  if (currentUser && currentUser.invitationCode) {
    codeInput.value = currentUser.invitationCode;
  } else {
    try {
      const me = await apiRequest("/auth/me");
      if (me && me.invitationCode) {
        codeInput.value = me.invitationCode;
        currentUser.invitationCode = me.invitationCode;
      }
    } catch (ignored) {}
  }

  // Load family links list
  try {
    const links = await apiRequest("/family/links");
    const tbody = document.getElementById("student-links-tbody");
    tbody.innerHTML = "";

    if (!links || links.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" style="text-align: center; color: var(--text-muted); padding: 24px;">No parent link requests received.</td></tr>`;
      return;
    }

    links.forEach(l => {
      const tr = document.createElement("tr");
      let statusBadge = "";
      let actions = "";

      if (l.status === "ACTIVE") {
        statusBadge = `<span class="badge badge-active">ACTIVE</span>`;
        actions = `<button class="btn btn-danger btn-sm" onclick="revokeLink(${l.id})">Revoke Access</button>`;
      } else if (l.status === "PENDING") {
        statusBadge = `<span class="badge badge-pending">PENDING APPROVAL</span>`;
        actions = `
          <button class="btn btn-success btn-sm" onclick="approveLink(${l.id})">Approve</button>
          <button class="btn btn-secondary btn-sm" onclick="revokeLink(${l.id})">Reject</button>
        `;
      } else {
        statusBadge = `<span class="badge badge-revoked">REVOKED</span>`;
        actions = `<span style="font-size: 11px; color: var(--text-muted);">Access Terminated</span>`;
      }

      tr.innerHTML = `
        <td><strong>${escapeHtml(l.parentName)}</strong></td>
        <td>${escapeHtml(l.parentEmail)}</td>
        <td>${statusBadge}</td>
        <td>${formatDate(l.createdAt)}</td>
        <td>${actions}</td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    console.error("Failed to load student links:", err);
  }
}

async function loadParentLinkStatus() {
  const card = document.getElementById("parent-link-status-card");
  try {
    const links = await apiRequest("/family/links");
    if (!links || links.length === 0) {
      card.innerHTML = `
        <div style="background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 24px; text-align: center; color: var(--text-secondary);">
          <p>You have not sent any link requests yet. Enter a Student Email or Code above to connect.</p>
        </div>
      `;
      return;
    }

    const currentLink = links[0]; // Primary link
    let statusContent = "";

    if (currentLink.status === "ACTIVE") {
      statusContent = `
        <div style="background: rgba(37, 214, 149, 0.08); border: 1px solid var(--success-border); border-radius: var(--radius-lg); padding: 22px; backdrop-filter: var(--blur-card);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <span class="badge badge-active" style="margin-bottom: 8px;">ACTIVE LINK</span>
              <h4 style="font-size: 16px; font-weight: 700; color: var(--text-main);">Linked to: ${escapeHtml(currentLink.studentName)}</h4>
              <p style="font-size: 13px; color: var(--text-secondary); margin-top: 4px;">Student Email: ${escapeHtml(currentLink.studentEmail)}</p>
              <p style="font-size: 11px; color: var(--success); margin-top: 4px;">Approved on: ${formatDate(currentLink.approvedAt)} • Read-only visibility enabled</p>
            </div>
            <button class="btn btn-danger btn-sm" onclick="revokeLink(${currentLink.id})">Disconnect Link</button>
          </div>
        </div>
      `;
    } else if (currentLink.status === "PENDING") {
      statusContent = `
        <div style="background: rgba(255, 184, 77, 0.08); border: 1px solid var(--warning-border); border-radius: var(--radius-lg); padding: 22px; backdrop-filter: var(--blur-card);">
          <div style="display: flex; align-items: center; justify-content: space-between;">
            <div>
              <span class="badge badge-pending" style="margin-bottom: 8px;">REQUEST PENDING</span>
              <h4 style="font-size: 15px; font-weight: 700; color: var(--warning);">Awaiting Student Approval</h4>
              <p style="font-size: 13px; color: var(--text-secondary); margin-top: 4px;">Request sent to: ${escapeHtml(currentLink.studentEmail)}</p>
              <p style="font-size: 11px; color: #f59e0b; margin-top: 4px;">Zero student data is visible while in pending state.</p>
            </div>
            <button class="btn btn-secondary btn-sm" onclick="revokeLink(${currentLink.id})">Cancel Request</button>
          </div>
        </div>
      `;
    } else {
      statusContent = `
        <div style="background: var(--bg-glass-card); backdrop-filter: var(--blur-card); border: 1px solid var(--border); border-radius: var(--radius-lg); padding: 22px; text-align: center;">
          <span class="badge badge-revoked" style="margin-bottom: 8px;">REVOKED</span>
          <p style="font-size: 13px; color: var(--text-secondary);">The link with ${escapeHtml(currentLink.studentEmail)} has been revoked. Send a new request above if needed.</p>
        </div>
      `;
    }

    card.innerHTML = statusContent;
  } catch (err) {
    console.error("Failed to load parent link status:", err);
  }
}

async function submitParentLinkRequest() {
  const input = document.getElementById("parent-link-input");
  const identifier = input.value.trim();
  if (!identifier) {
    showToast("Please enter a Student email or invitation code.", "warning");
    return;
  }

  try {
    await apiRequest("/family/request", "POST", { identifier });
    showToast("Link request sent! Waiting for Student approval.", "success");
    input.value = "";
    loadParentLinkStatus();
  } catch (err) {
    // Handled in api.js
  }
}

async function approveLink(linkId) {
  try {
    await apiRequest(`/family/links/${linkId}/approve`, "POST");
    showToast("Parent link approved successfully! Read-only visibility granted.", "success");
    loadStudentLinkRequests();
  } catch (err) {
    // Handled in api.js
  }
}

async function revokeLink(linkId) {
  try {
    await apiRequest(`/family/links/${linkId}/revoke`, "POST");
    showToast("Link revoked immediately. Access has been removed.", "info");
    loadFamilyLinks();
    loadDashboard();
  } catch (err) {
    // Handled in api.js
  }
}

function copyInviteCode() {
  const codeInput = document.getElementById("student-invite-code-input");
  if (codeInput && codeInput.value) {
    navigator.clipboard.writeText(codeInput.value).then(() => {
      showToast("Invitation code copied to clipboard!", "success");
    }).catch(() => {
      codeInput.select();
      document.execCommand("copy");
      showToast("Invitation code copied!", "success");
    });
  }
}
