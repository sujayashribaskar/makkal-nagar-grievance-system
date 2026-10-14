/**
 * Makkal Nagar Municipal Corporation - Shared Frontend Logic
 * API communication, session management, badges, and UI helpers
 */

// Auto-detect API Base URL: If served from localhost:8080 use relative path, else default to http://localhost:8080
const API_BASE_URL = (window.location.origin.includes('localhost:8080') || window.location.origin.includes('127.0.0.1:8080'))
    ? ''
    : 'http://localhost:8080';

/**
 * Universal fetch wrapper supporting JSON payloads, session cookies, and standard error handling.
 */
async function apiFetch(endpoint, options = {}) {
    const url = endpoint.startsWith('http') ? endpoint : `${API_BASE_URL}${endpoint}`;

    const defaultHeaders = {
        'Accept': 'application/json'
    };

    if (options.body && typeof options.body === 'object' && !(options.body instanceof FormData)) {
        defaultHeaders['Content-Type'] = 'application/json';
        options.body = JSON.stringify(options.body);
    }

    const config = {
        ...options,
        headers: {
            ...defaultHeaders,
            ...(options.headers || {})
        },
        credentials: 'include' // Sends session cookies across origins
    };

    try {
        const response = await fetch(url, config);

        // Handle CSV / text responses
        const contentType = response.headers.get('content-type');
        if (contentType && contentType.includes('text/csv')) {
            if (!response.ok) throw new Error(`HTTP error ${response.status}`);
            return await response.text();
        }

        if (response.status === 204) {
            return null;
        }

        const data = await response.json().catch(() => null);

        if (!response.ok) {
            const errorMsg = data && (data.message || data.error) ?
                data.message || data.error : `Request failed with status ${response.status}`;
            const err = new Error(errorMsg);
            err.status = response.status;
            err.data = data;
            throw err;
        }

        return data;
    } catch (err) {
        console.error(`API Error on ${endpoint}:`, err);
        throw err;
    }
}

/**
 * Check if admin is currently authenticated.
 */
async function checkAdminAuth() {
    try {
        const user = await apiFetch('/api/auth/me');
        return user;
    } catch (err) {
        return null;
    }
}

/**
 * Require admin authentication on protected pages. Redirects to login.html if unauthenticated.
 */
async function requireAdminAuth() {
    const user = await checkAdminAuth();
    if (!user) {
        sessionStorage.setItem('redirect_after_login', window.location.pathname + window.location.search);
        window.location.href = 'login.html';
        return null;
    }
    updateNavUserUI(user);
    return user;
}

/**
 * Performs admin logout.
 */
async function logout() {
    try {
        await apiFetch('/api/auth/logout', { method: 'POST' });
    } catch (e) {
        console.warn('Logout notification error:', e);
    }
    window.location.href = 'login.html';
}

/**
 * Updates top navigation bar with user profile or login button.
 */
function updateNavUserUI(user) {
    const userContainer = document.getElementById('navUserContainer');
    if (!userContainer) return;

    if (user) {
        userContainer.innerHTML = `
            <div class="dropdown">
                <button class="btn btn-sm btn-outline-light dropdown-toggle d-flex align-items-center gap-2" type="button" data-bs-toggle="dropdown">
                    <span class="badge bg-teal rounded-pill"><i class="bi bi-shield-check"></i></span>
                    <span>${escapeHtml(user.fullName || user.username)}</span>
                </button>
                <ul class="dropdown-menu dropdown-menu-end shadow">
                    <li><h6 class="dropdown-header">Logged in as Administrator</h6></li>
                    <li><a class="dropdown-item" href="dashboard.html"><i class="bi bi-speedometer2 me-2"></i>Dashboard</a></li>
                    <li><a class="dropdown-item" href="complaints.html"><i class="bi bi-card-checklist me-2"></i>All Complaints</a></li>
                    <li><a class="dropdown-item" href="departments.html"><i class="bi bi-diagram-3 me-2"></i>Departments</a></li>
                    <li><a class="dropdown-item" href="reports.html"><i class="bi bi-graph-up me-2"></i>Reports</a></li>
                    <li><hr class="dropdown-divider"></li>
                    <li><button class="dropdown-item text-danger" onclick="logout()"><i class="bi bi-box-arrow-right me-2"></i>Sign Out</button></li>
                </ul>
            </div>
        `;
    } else {
        userContainer.innerHTML = `
            <a href="login.html" class="btn btn-sm btn-outline-light">
                <i class="bi bi-box-arrow-in-right me-1"></i> Admin Portal
            </a>
        `;
    }
}

/**
 * UI Badges and formatters
 */
function getCategoryBadge(category) {
    const labels = {
        'WATER': 'Water Supply',
        'ROAD': 'Roads & Infrastructure',
        'STREETLIGHT': 'Streetlights',
        'WASTE': 'Waste Management'
    };
    return `<span class="badge badge-category-${category} px-2 py-1">${labels[category] || category}</span>`;
}

function getSeverityBadge(severity) {
    const labels = {
        'CRITICAL': 'CRITICAL',
        'HIGH': 'HIGH',
        'MEDIUM': 'MEDIUM',
        'LOW': 'LOW'
    };
    return `<span class="badge badge-severity-${severity} px-2 py-1">${labels[severity] || severity}</span>`;
}

function getStatusBadge(status) {
    const labels = {
        'REGISTERED': 'Registered / Queue',
        'ASSIGNED': 'Assigned',
        'IN_PROGRESS': 'In Progress',
        'RESOLVED': 'Resolved'
    };
    return `<span class="badge badge-status-${status} px-2 py-1">${labels[status] || status}</span>`;
}

function formatDate(isoString) {
    if (!isoString) return '—';
    try {
        const d = new Date(isoString);
        return d.toLocaleDateString('en-IN', {
            year: 'numeric',
            month: 'short',
            day: 'numeric',
            hour: '2-digit',
            minute: '2-digit'
        });
    } catch {
        return isoString;
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return text
        .replace(/&/g, "&amp;")
        .replace(/</g, "&lt;")
        .replace(/>/g, "&gt;")
        .replace(/"/g, "&quot;")
        .replace(/'/g, "&#039;");
}

function showAlert(containerId, message, type = 'danger') {
    const container = document.getElementById(containerId);
    if (!container) return;
    container.innerHTML = `
        <div class="alert alert-${type} alert-dismissible fade show mn-alert" role="alert">
            <i class="bi bi-${type === 'danger' ? 'exclamation-octagon' : 'check-circle'} me-2"></i>
            ${escapeHtml(message)}
            <button type="button" class="btn-close" data-bs-dismiss="alert" aria-label="Close"></button>
        </div>
    `;
}
