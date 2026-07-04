const API_BASE = window.location.origin + '/api/v1/public';

const stepEmail = document.getElementById('step-email');
const stepOtp = document.getElementById('step-otp');
const stepOrgs = document.getElementById('step-orgs');
const stepCheckout = document.getElementById('step-checkout');
const alertBox = document.getElementById('alert');
const emailForm = document.getElementById('email-form');
const otpForm = document.getElementById('otp-form');
const checkoutForm = document.getElementById('checkout-form');
const otpEmailLabel = document.getElementById('otp-email');
const orgList = document.getElementById('org-list');
const checkoutOrgName = document.getElementById('checkout-org-name');
const checkoutHint = document.getElementById('checkout-hint');
const backToEmailBtn = document.getElementById('back-to-email');
const backToOrgsBtn = document.getElementById('back-to-orgs');

let currentEmail = '';
let sessionId = null;
let selectedOrganization = null;

function sanitizeEmail(value) {
    let email = String(value || '').trim().toLowerCase();

    while (email.length > 0 && [':', ';', ','].includes(email.charAt(email.length - 1))) {
        email = email.slice(0, -1);
    }

    return email;
}

function isValidEmail(email) {
    return /^[a-z0-9._%+-]+@[a-z0-9.-]+\.[a-z]{2,}$/i.test(email);
}

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
    stepCheckout.classList.add('hidden');
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
        currentEmail = sanitizeEmail(document.getElementById('email').value);

        if (!isValidEmail(currentEmail)) {
            throw new Error('Ingresá un email válido');
        }

        document.getElementById('email').value = currentEmail;
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

checkoutForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    if (!selectedOrganization) {
        showAlert('Elegí una organización primero');
        return;
    }

    const submitBtn = document.getElementById('checkout-btn');
    submitBtn.disabled = true;

    try {
        const quantity = Number(document.getElementById('quantity').value || 1);
        const checkout = await apiPost('/checkout/store-addon', {
            sessionId,
            organizationId: selectedOrganization.id,
            quantity,
        });

        if (!checkout.checkoutUrl) {
            throw new Error(checkout.message || 'No se pudo iniciar el checkout');
        }

        window.location.href = checkout.checkoutUrl;
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

backToOrgsBtn.addEventListener('click', () => {
    selectedOrganization = null;
    showStep(stepOrgs);
});

async function loadOrganizations() {
    orgList.innerHTML = '';
    const organizations = await apiGet(`/organizations?sessionId=${sessionId}`);

    if (!organizations.length) {
        orgList.innerHTML = '<p>No hay organizaciones disponibles para este usuario.</p>';
        checkoutHint.textContent = '';
        return;
    }

    checkoutHint.textContent = 'Seleccioná una organización para continuar con el checkout.';

    organizations.forEach((org) => {
        const card = document.createElement('button');
        card.type = 'button';
        card.className = 'org-card org-card-btn';
        card.innerHTML = `
            <h2>${escapeHtml(org.displayName || org.name)}</h2>
            <div class="org-meta">${escapeHtml(org.countryCode || '-')} · ${escapeHtml(org.currency || '-')} · ${escapeHtml(org.role)}</div>
        `;
        card.addEventListener('click', () => openCheckout(org));
        orgList.appendChild(card);
    });
}

function openCheckout(org) {
    selectedOrganization = org;
    checkoutOrgName.textContent = `Organización: ${org.displayName || org.name}`;

    const countryCode = String(org.countryCode || '').toUpperCase();
    const currency = String(org.currency || '').toUpperCase();
    const isArgentina = countryCode === 'AR';
    const isStripe = countryCode === 'ES' || currency === 'EUR';

    const checkoutBtn = document.getElementById('checkout-btn');
    checkoutBtn.disabled = !(isArgentina || isStripe);

    if (isArgentina) {
        checkoutBtn.textContent = 'Pagar con Mercado Pago';
    } else if (isStripe) {
        checkoutBtn.textContent = 'Pagar con Stripe';
    } else {
        checkoutBtn.textContent = 'Checkout no disponible';
        showAlert('Checkout no disponible para esta organización.', 'error');
    }

    document.getElementById('quantity').value = '1';
    showStep(stepCheckout);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#39;');
}

const urlParams = new URLSearchParams(window.location.search);
const paymentStatus = urlParams.get('status');
if (paymentStatus) {
    showAlert(
        paymentStatus === 'success'
            ? 'Pago recibido. La activación puede tardar unos segundos.'
            : paymentStatus === 'pending'
                ? 'Pago pendiente. Te avisaremos cuando se acredite.'
                : 'El pago no se completó. Podés intentar nuevamente.',
        paymentStatus === 'success' ? 'success' : 'error'
    );
}
