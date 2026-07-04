const API_BASE = window.location.origin + '/api/v1/public';

const stepEmail = document.getElementById('step-email');
const stepOtp = document.getElementById('step-otp');
const stepOrgs = document.getElementById('step-orgs');
const alertBox = document.getElementById('alert');
const emailForm = document.getElementById('email-form');
const otpForm = document.getElementById('otp-form');
const otpEmailLabel = document.getElementById('otp-email');
const orgList = document.getElementById('org-list');
const backToEmailBtn = document.getElementById('back-to-email');

let currentEmail = '';
let sessionId = null;

function showAlert(message, type = 'error') {
    alertBox.textContent = message;
    alertBox.className = `alert ${type}`;
    alertBox.classList.remove('hidden');
}

function hideAlert() {
    alertBox.classList.add('hidden');
}

function showStep(step) {
    stepEmail.classList.add('hidden');
    stepOtp.classList.add('hidden');
    stepOrgs.classList.add('hidden');
    step.classList.remove('hidden');
    hideAlert();
}

async function apiPost(path, body) {
    const response = await fetch(`${API_BASE}${path}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(body),
    });

    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || 'Request failed');
    }
    return data;
}

async function apiGet(path) {
    const response = await fetch(`${API_BASE}${path}`);
    const data = await response.json().catch(() => ({}));
    if (!response.ok) {
        throw new Error(data.message || 'Request failed');
    }
    return data;
}

emailForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    const submitBtn = document.getElementById('send-code-btn');
    submitBtn.disabled = true;

    try {
        currentEmail = document.getElementById('email').value.trim();
        await apiPost('/verify-email', { email: currentEmail });
        otpEmailLabel.textContent = currentEmail;
        showStep(stepOtp);
        showAlert('Código enviado. Revisá tu bandeja de entrada.', 'success');
    } catch (error) {
        showAlert(error.message);
    } finally {
        submitBtn.disabled = false;
    }
});

otpForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    const submitBtn = document.getElementById('confirm-code-btn');
    submitBtn.disabled = true;

    try {
        const otp = document.getElementById('otp').value.trim();
        const session = await apiPost('/confirm-email', { email: currentEmail, otp });
        sessionId = session.sessionId;
        await loadOrganizations();
        showStep(stepOrgs);
    } catch (error) {
        showAlert(error.message);
    } finally {
        submitBtn.disabled = false;
    }
});

backToEmailBtn.addEventListener('click', () => {
    document.getElementById('otp').value = '';
    showStep(stepEmail);
});

async function loadOrganizations() {
    orgList.innerHTML = '';
    const organizations = await apiGet(`/organizations?sessionId=${sessionId}`);

    if (!organizations.length) {
        orgList.innerHTML = '<p>No hay organizaciones disponibles para este usuario.</p>';
        return;
    }

    organizations.forEach((org) => {
        const card = document.createElement('article');
        card.className = 'org-card';
        card.innerHTML = `
            <h2>${escapeHtml(org.displayName || org.name)}</h2>
            <div class="org-meta">${escapeHtml(org.countryCode || '-')} · ${escapeHtml(org.currency || '-')} · ${escapeHtml(org.role)}</div>
        `;
        orgList.appendChild(card);
    });
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}
