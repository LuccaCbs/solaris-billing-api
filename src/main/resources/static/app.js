const API_BASE = window.location.origin + '/api/v1/public';

const stepEmail = document.getElementById('step-email');
const stepOtp = document.getElementById('step-otp');
const stepOrgs = document.getElementById('step-orgs');
const stepPlans = document.getElementById('step-plans');
const stepFreemiumRedeem = document.getElementById('step-freemium-redeem');
const stepCheckout = document.getElementById('step-checkout');
const alertBox = document.getElementById('alert');
const emailForm = document.getElementById('email-form');
const otpForm = document.getElementById('otp-form');
const checkoutForm = document.getElementById('checkout-form');
const freemiumForm = document.getElementById('freemium-form');
const otpEmailLabel = document.getElementById('otp-email');
const orgList = document.getElementById('org-list');
const planGrid = document.getElementById('plan-grid');
const checkoutPanel = document.getElementById('checkout-panel');
const checkoutPanelTitle = document.getElementById('checkout-panel-title');
const checkoutPanelPlan = document.getElementById('checkout-panel-plan');
const checkoutOrgName = document.getElementById('checkout-org-name');
const plansOrgName = document.getElementById('plans-org-name');
const freemiumOrgName = document.getElementById('freemium-org-name');
const checkoutHint = document.getElementById('checkout-hint');
const promoPreview = document.getElementById('promo-preview');
const freemiumPromoPreview = document.getElementById('freemium-promo-preview');
const backToEmailBtn = document.getElementById('back-to-email');
const backToOrgsBtn = document.getElementById('back-to-orgs');
const backToOrgsFromPlansBtn = document.getElementById('back-to-orgs-from-plans');
const backToPlansFromAddonBtn = document.getElementById('back-to-plans-from-addon');
const backToPlansFromFreemiumBtn = document.getElementById('back-to-plans-from-freemium');
const previewPromoBtn = document.getElementById('preview-promo-btn');
const previewFreemiumPromoBtn = document.getElementById('preview-freemium-promo-btn');
const subscribeBtn = document.getElementById('subscribe-btn');
const redeemFreemiumBtn = document.getElementById('redeem-freemium-btn');
const openAddonBtn = document.getElementById('open-addon-btn');

const FALLBACK_FREEMIUM = {
    code: 'POS',
    displayName: 'Freemium / POS',
    tagline: 'Ideal para empezar sin costo',
    features: [
        'Punto de venta básico',
        'Inventario simple',
        '1 sucursal',
        'Activación con código promocional',
    ],
};

const FALLBACK_PLAN_META = {
    BUSINESS: {
        tagline: 'Ideal para negocios en crecimiento',
        recommended: true,
        features: [
            'Punto de venta completo',
            'Inventario y productos',
            'Clientes y CRM',
            'Facturación fiscal',
            'Equipo y roles',
            'Auditoría de cambios',
            'Reportes y analytics',
            '1 sucursal incluida',
        ],
    },
    SCALE: {
        tagline: 'Ideal para operaciones multi-sucursal',
        recommended: false,
        features: [
            'Todo lo incluido en Business',
            'Multi-sucursal',
            'Gestión centralizada',
            'Analytics avanzados',
            '1 sucursal incluida',
        ],
    },
};

let currentEmail = '';
let sessionId = null;
let selectedOrganization = null;
let selectedPlan = null;
let freemiumPlan = null;
let promoPreviewData = null;
let freemiumPromoPreviewData = null;

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
    stepPlans.classList.add('hidden');
    stepFreemiumRedeem.classList.add('hidden');
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

function formatPrice(price, currency) {
    const amount = Number(price);
    if (!Number.isFinite(amount) || amount <= 0) {
        return null;
    }

    if (currency === 'ARS') {
        return `${amount.toLocaleString('es-AR')} ARS`;
    }

    return `${amount.toLocaleString('es-ES', { minimumFractionDigits: 0, maximumFractionDigits: 2 })} ${currency}`;
}

function resetCheckoutPanel() {
    selectedPlan = null;
    promoPreviewData = null;
    document.getElementById('promo-code').value = '';
    promoPreview.textContent = '';
    promoPreview.className = 'hint';
    checkoutPanel.classList.add('hidden');
    checkoutPanelPlan.textContent = '';
    subscribeBtn.disabled = false;
}

function resetFreemiumForm() {
    freemiumPromoPreviewData = null;
    document.getElementById('freemium-promo-code').value = '';
    freemiumPromoPreview.textContent = '';
    freemiumPromoPreview.className = 'hint';
    redeemFreemiumBtn.disabled = false;
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
        await loadOrganizations(session.preferredOrganizationId ?? null);
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

freemiumForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    hideAlert();

    if (!selectedOrganization || !freemiumPlan) {
        showAlert('Elegí una organización primero');
        return;
    }

    const promoCode = document.getElementById('freemium-promo-code').value.trim();
    if (!promoCode) {
        showAlert('Ingresá un código promocional');
        return;
    }

    redeemFreemiumBtn.disabled = true;

    try {
        const checkout = await apiPost('/checkout/subscription', {
            sessionId,
            organizationId: selectedOrganization.id,
            planCode: freemiumPlan.code,
            promoCode,
        });

        if (checkout.status === 'FULFILLED' && checkout.redirectUrl) {
            window.location.href = checkout.redirectUrl;
            return;
        }

        if (!checkout.checkoutUrl) {
            throw new Error(checkout.message || 'No se pudo activar el plan');
        }

        window.location.href = checkout.checkoutUrl;
    } catch (error) {
        showAlert(error.message);
    } finally {
        redeemFreemiumBtn.disabled = false;
    }
});

previewPromoBtn.addEventListener('click', async () => {
    hideAlert();

    if (!selectedOrganization || !selectedPlan) {
        showAlert('Elegí un plan primero');
        return;
    }

    const promoCode = document.getElementById('promo-code').value.trim();
    if (!promoCode) {
        promoPreviewData = null;
        promoPreview.textContent = '';
        promoPreview.className = 'hint';
        return;
    }

    try {
        promoPreviewData = await apiPost('/promo-codes/preview', {
            sessionId,
            organizationId: selectedOrganization.id,
            planCode: selectedPlan.code,
            promoCode,
        });

        promoPreview.textContent = promoPreviewData.requiresPayment
            ? `Precio final: ${promoPreviewData.finalPrice} ${promoPreviewData.currency}`
            : 'Cupón válido: activación gratuita sin pasarela de pago.';
        promoPreview.className = 'hint success';
    } catch (error) {
        promoPreviewData = null;
        promoPreview.textContent = '';
        promoPreview.className = 'hint';
        showAlert(error.message);
    }
});

previewFreemiumPromoBtn.addEventListener('click', async () => {
    hideAlert();

    if (!selectedOrganization || !freemiumPlan) {
        showAlert('Elegí una organización primero');
        return;
    }

    const promoCode = document.getElementById('freemium-promo-code').value.trim();
    if (!promoCode) {
        freemiumPromoPreviewData = null;
        freemiumPromoPreview.textContent = '';
        freemiumPromoPreview.className = 'hint';
        return;
    }

    try {
        freemiumPromoPreviewData = await apiPost('/promo-codes/preview', {
            sessionId,
            organizationId: selectedOrganization.id,
            planCode: freemiumPlan.code,
            promoCode,
        });

        const grantedPlan = freemiumPromoPreviewData.effectivePlanCode || freemiumPlan.code;
        freemiumPromoPreview.textContent = freemiumPromoPreviewData.requiresPayment
            ? `Este código requiere pago (${freemiumPromoPreviewData.finalPrice} ${freemiumPromoPreviewData.currency}). Usá un código de activación gratuita.`
            : `Código válido. Se activará el plan ${grantedPlan} sin costo.`;
        freemiumPromoPreview.className = freemiumPromoPreviewData.requiresPayment ? 'hint' : 'hint success';
    } catch (error) {
        freemiumPromoPreviewData = null;
        freemiumPromoPreview.textContent = '';
        freemiumPromoPreview.className = 'hint';
        showAlert(error.message);
    }
});

subscribeBtn.addEventListener('click', async () => {
    hideAlert();

    if (!selectedOrganization || !selectedPlan) {
        showAlert('Elegí un plan primero');
        return;
    }

    subscribeBtn.disabled = true;

    try {
        const promoCode = document.getElementById('promo-code').value.trim();
        const checkout = await apiPost('/checkout/subscription', {
            sessionId,
            organizationId: selectedOrganization.id,
            planCode: selectedPlan.code,
            promoCode: promoCode || undefined,
        });

        if (checkout.status === 'FULFILLED' && checkout.redirectUrl) {
            window.location.href = checkout.redirectUrl;
            return;
        }

        if (!checkout.checkoutUrl) {
            throw new Error(checkout.message || 'No se pudo iniciar el checkout');
        }

        window.location.href = checkout.checkoutUrl;
    } catch (error) {
        showAlert(error.message);
    } finally {
        subscribeBtn.disabled = false;
    }
});

openAddonBtn.addEventListener('click', () => {
    if (!selectedOrganization) return;
    openAddonCheckout(selectedOrganization);
});

backToEmailBtn.addEventListener('click', () => {
    document.getElementById('otp').value = '';
    showStep(stepEmail);
});

backToOrgsBtn.addEventListener('click', () => {
    selectedOrganization = null;
    resetCheckoutPanel();
    showStep(stepOrgs);
});

backToOrgsFromPlansBtn.addEventListener('click', () => {
    selectedOrganization = null;
    resetCheckoutPanel();
    showStep(stepOrgs);
});

backToPlansFromAddonBtn.addEventListener('click', () => {
    if (selectedOrganization) {
        void openPlans(selectedOrganization, { preserveSelection: true });
    } else {
        showStep(stepOrgs);
    }
});

backToPlansFromFreemiumBtn.addEventListener('click', () => {
    resetFreemiumForm();
    if (selectedOrganization) {
        void openPlans(selectedOrganization, { preserveSelection: true });
    } else {
        showStep(stepOrgs);
    }
});

async function loadOrganizations(preferredOrganizationId = null) {
    orgList.innerHTML = '';
    const organizations = await apiGet(`/organizations?sessionId=${sessionId}`);

    if (!organizations.length) {
        orgList.innerHTML = '<p>No hay organizaciones disponibles para este usuario.</p>';
        checkoutHint.textContent = '';
        return organizations;
    }

    checkoutHint.textContent = 'Seleccioná una organización para continuar.';

    if (preferredOrganizationId != null) {
        const preferred = organizations.find((org) => org.id === preferredOrganizationId);
        if (preferred) {
            await openPlans(preferred);
            return organizations;
        }
    }

    organizations.forEach((org) => {
        const card = document.createElement('button');
        card.type = 'button';
        card.className = 'org-card org-card-btn';
        card.innerHTML = `
            <h2>${escapeHtml(org.displayName || org.name)}</h2>
            <div class="org-meta">${escapeHtml(org.countryCode || '-')} · ${escapeHtml(org.currency || '-')} · ${escapeHtml(org.role)}</div>
        `;
        card.addEventListener('click', () => openPlans(org));
        orgList.appendChild(card);
    });

    return organizations;
}

function buildFeatureList(features) {
    const list = document.createElement('ul');
    list.className = 'plan-features';
    (features || []).forEach((feature) => {
        const item = document.createElement('li');
        item.textContent = feature;
        list.appendChild(item);
    });
    return list;
}

function createPlanCard(plan, { isFreemium = false, onSelect } = {}) {
    const card = document.createElement('article');
    card.className = 'plan-card';
    card.dataset.planCode = plan.code;

    if (plan.recommended) {
        card.classList.add('plan-card--featured');
        const ribbon = document.createElement('div');
        ribbon.className = 'plan-ribbon';
        ribbon.textContent = 'Más vendido';
        card.appendChild(ribbon);
    }

    if (isFreemium) {
        card.classList.add('plan-card--freemium');
    }

    const name = document.createElement('h2');
    name.className = 'plan-name';
    name.textContent = plan.displayName || plan.code;
    card.appendChild(name);

    const tagline = document.createElement('p');
    tagline.className = 'plan-tagline';
    tagline.textContent = plan.tagline || plan.description || '';
    card.appendChild(tagline);

    const priceBlock = document.createElement('div');
    priceBlock.className = 'plan-price-block';

    const price = document.createElement('div');
    price.className = 'plan-price';

    if (isFreemium) {
        price.classList.add('plan-price--free');
        price.textContent = 'Gratis';
    } else {
        const formatted = formatPrice(plan.price, plan.currency);
        price.textContent = formatted || `${plan.price} ${plan.currency}`;
    }

    priceBlock.appendChild(price);

    if (!isFreemium) {
        const period = document.createElement('span');
        period.className = 'plan-price-period';
        period.textContent = '/ mes';
        priceBlock.appendChild(period);
    }

    card.appendChild(priceBlock);
    card.appendChild(buildFeatureList(plan.features));

    const button = document.createElement('button');
    button.type = 'button';
    button.className = isFreemium ? 'plan-select-btn plan-select-btn--primary' : 'plan-select-btn';
    button.textContent = isFreemium ? 'Activar con código' : 'Elegir plan';
    button.addEventListener('click', () => onSelect(plan, card));
    card.appendChild(button);

    return card;
}

function selectPaidPlan(plan, cardElement) {
    selectedPlan = plan;
    promoPreviewData = null;
    document.getElementById('promo-code').value = '';
    promoPreview.textContent = '';
    promoPreview.className = 'hint';

    planGrid.querySelectorAll('.plan-card').forEach((card) => card.classList.remove('selected'));
    cardElement.classList.add('selected');

    checkoutPanel.classList.remove('hidden');
    checkoutPanelTitle.textContent = 'Completar suscripción';
    checkoutPanelPlan.textContent = `Plan seleccionado: ${plan.displayName || plan.code}`;
}

function openFreemiumRedeem() {
    resetFreemiumForm();
    freemiumOrgName.textContent = `Organización: ${selectedOrganization.displayName || selectedOrganization.name}`;
    showStep(stepFreemiumRedeem);
}

async function openPlans(org, options = {}) {
    const preserveSelection = options.preserveSelection === true;
    selectedOrganization = org;

    if (!preserveSelection) {
        resetCheckoutPanel();
    }

    plansOrgName.textContent = `Organización: ${org.displayName || org.name}`;
    planGrid.innerHTML = '<p>Cargando planes...</p>';

    try {
        const catalog = await apiGet(
            `/plans?sessionId=${sessionId}&organizationId=${org.id}`
        );

        freemiumPlan = catalog.freemiumPlan || FALLBACK_FREEMIUM;
        planGrid.innerHTML = '';

        const plans = catalog.plans || [];
        if (!plans.length && !freemiumPlan) {
            planGrid.innerHTML = '<p>No hay planes disponibles.</p>';
        } else {
            plans.forEach((plan) => {
                const fallback = FALLBACK_PLAN_META[plan.code] || {};
                const enrichedPlan = {
                    ...plan,
                    tagline: plan.tagline || fallback.tagline || plan.description || '',
                    recommended: plan.recommended ?? fallback.recommended ?? false,
                    features: plan.features?.length ? plan.features : (fallback.features || []),
                };

                const card = createPlanCard(enrichedPlan, {
                    onSelect: (selected, cardEl) => selectPaidPlan(selected, cardEl),
                });
                planGrid.appendChild(card);

                if (preserveSelection && selectedPlan && selectedPlan.code === plan.code) {
                    selectPaidPlan(enrichedPlan, card);
                }
            });

            if (freemiumPlan) {
                const freemiumCard = createPlanCard(freemiumPlan, {
                    isFreemium: true,
                    onSelect: () => openFreemiumRedeem(),
                });
                planGrid.insertBefore(freemiumCard, planGrid.firstChild);
            }
        }

        showStep(stepPlans);
    } catch (error) {
        showAlert(error.message);
    }
}

function openAddonCheckout(org) {
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

async function tryAppBillingTokenLogin() {
    const billingToken = urlParams.get('billingToken');
    if (!billingToken) {
        return false;
    }

    try {
        const session = await apiPost('/session/from-app-token', { billingToken });
        currentEmail = sanitizeEmail(session.email);
        sessionId = session.sessionId;
        document.getElementById('email').value = currentEmail;
        await loadOrganizations(session.preferredOrganizationId ?? null);
        showStep(stepOrgs);
        showAlert(`Sesión iniciada como ${currentEmail}`, 'success');
        return true;
    } catch (error) {
        showAlert(
            error.message || 'No se pudo usar el enlace de la app. Ingresá tu email manualmente.'
        );
        showStep(stepEmail);
        return false;
    }
}

void tryAppBillingTokenLogin();
