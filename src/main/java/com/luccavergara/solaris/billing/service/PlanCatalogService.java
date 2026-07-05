package com.luccavergara.solaris.billing.service;

import com.luccavergara.solaris.billing.billing.BillingPricingService;
import com.luccavergara.solaris.billing.dto.PlanCatalogResponse;
import com.luccavergara.solaris.billing.entity.Organization;
import com.luccavergara.solaris.billing.entity.SubscriptionPlan;
import com.luccavergara.solaris.billing.entity.SubscriptionPlanCode;
import com.luccavergara.solaris.billing.repository.SubscriptionPlanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PlanCatalogService {

    private static final Map<SubscriptionPlanCode, PlanPresentation> PLAN_PRESENTATION = Map.of(
            SubscriptionPlanCode.BUSINESS,
            new PlanPresentation(
                    "Ideal para negocios en crecimiento",
                    true,
                    List.of(
                            "Punto de venta completo",
                            "Inventario y productos",
                            "Clientes y CRM",
                            "Facturación fiscal",
                            "Equipo y roles",
                            "Auditoría de cambios",
                            "Reportes y analytics",
                            "1 sucursal incluida"
                    )
            ),
            SubscriptionPlanCode.SCALE,
            new PlanPresentation(
                    "Ideal para operaciones multi-sucursal",
                    false,
                    List.of(
                            "Todo lo incluido en Business",
                            "Multi-sucursal",
                            "Gestión centralizada",
                            "Analytics avanzados",
                            "1 sucursal incluida"
                    )
            )
    );

    private static final PlanCatalogResponse.FreemiumPlanResponse FREEMIUM_PLAN =
            PlanCatalogResponse.FreemiumPlanResponse.builder()
                    .code(SubscriptionPlanCode.POS)
                    .displayName("Freemium / POS")
                    .tagline("Ideal para empezar sin costo")
                    .features(List.of(
                            "Punto de venta básico",
                            "Inventario simple",
                            "1 sucursal",
                            "Activación con código promocional"
                    ))
                    .build();

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final BillingPricingService billingPricingService;

    @Transactional(readOnly = true)
    public PlanCatalogResponse getPublicPlans(Organization organization) {
        String currency = billingPricingService.resolveCurrency(organization);

        List<PlanCatalogResponse.PlanResponse> plans = subscriptionPlanRepository
                .findByIsPublicTrueAndActiveTrueOrderBySortOrderAsc()
                .stream()
                .filter(plan -> plan.getCode() != SubscriptionPlanCode.POS)
                .map(plan -> toPlanResponse(plan, organization, currency))
                .toList();

        return PlanCatalogResponse.builder()
                .plans(plans)
                .freemiumPlan(FREEMIUM_PLAN)
                .currency(currency)
                .build();
    }

    private PlanCatalogResponse.PlanResponse toPlanResponse(
            SubscriptionPlan plan,
            Organization organization,
            String currency
    ) {
        PlanPresentation presentation = PLAN_PRESENTATION.getOrDefault(
                plan.getCode(),
                new PlanPresentation(plan.getDescription(), false, List.of())
        );

        return PlanCatalogResponse.PlanResponse.builder()
                .code(plan.getCode())
                .displayName(plan.getDisplayName())
                .description(plan.getDescription())
                .tagline(presentation.tagline())
                .recommended(presentation.recommended())
                .features(presentation.features())
                .maxStores(plan.getMaxStores())
                .price(billingPricingService.getPlanPrice(organization, plan.getCode()))
                .currency(currency)
                .build();
    }

    private record PlanPresentation(String tagline, boolean recommended, List<String> features) {
    }
}
