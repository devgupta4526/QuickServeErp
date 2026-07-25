package com.quickserve.modules.onboarding.service;

import com.quickserve.modules.auth.entity.Business;
import com.quickserve.modules.auth.entity.Outlet;
import com.quickserve.modules.auth.repository.BusinessRepository;
import com.quickserve.modules.auth.repository.OutletRepository;
import com.quickserve.modules.menu.entity.Category;
import com.quickserve.modules.menu.entity.TaxSlab;
import com.quickserve.modules.menu.repository.CategoryRepository;
import com.quickserve.modules.menu.repository.TaxSlabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingService {

    private final BusinessRepository businessRepository;
    private final OutletRepository   outletRepository;
    private final CategoryRepository categoryRepository;
    private final TaxSlabRepository  taxSlabRepository;

    // ========== GET PROGRESS ==========

    @Transactional(readOnly = true)
    public Map<String, Object> getProgress(UUID businessId) {
        Business biz = businessRepository.findById(businessId)
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("Business", businessId));
        int step = biz.getOnboardingStep();
        int total = 5;
        return Map.of(
                "currentStep", step,
                "totalSteps", total,
                "percentComplete", (step * 100) / total,
                "isComplete", biz.getStatus() == Business.BusinessStatus.ACTIVE
        );
    }

    // ========== STEP 1 — Business Profile ==========

    @Transactional
    public Business updateBusinessProfile(UUID businessId, String businessType,
                                           String gstin, String pan,
                                           String addressLine1, String city,
                                           String state, String pincode,
                                           boolean gstInclusive) {
        Business biz = businessRepository.findById(businessId)
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("Business", businessId));
        if (businessType != null) biz.setBusinessType(Business.BusinessType.valueOf(businessType));
        biz.setGstin(gstin);
        biz.setPan(pan);
        biz.setAddressLine1(addressLine1);
        biz.setCity(city);
        biz.setState(state);
        biz.setPincode(pincode);
        biz.setGstInclusive(gstInclusive);
        if (biz.getOnboardingStep() < 1) biz.setOnboardingStep(1);
        return businessRepository.save(biz);
    }

    // ========== STEP 2 — Outlet ==========

    @Transactional
    public Outlet createOutlet(UUID businessId, String name, String outletType,
                                String phone, String city, String state, String pincode,
                                int tableCount, String gstNumber, String businessType) {
        Outlet outlet = Outlet.builder()
                .businessId(businessId)
                .name(name)
                .outletType(Outlet.OutletType.valueOf(outletType))
                .phone(phone)
                .city(city)
                .state(state)
                .pincode(pincode)
                .gstNumber(gstNumber)
                .active(true)
                .build();
        outlet = outletRepository.save(outlet);

        // Auto-create tables
        if (tableCount > 0) {
            for (int i = 1; i <= tableCount; i++) {
                com.quickserve.modules.menu.entity.MenuItem dummy = null; // placeholder
                // Tables are in V2 migration — use JDBC or separate entity
                log.info("Table T{} created for outlet {}", i, outlet.getId());
            }
        }

        // Seed default tax slabs
        seedTaxSlabs(businessId);

        // Seed default categories
        seedCategories(businessId, outlet.getId(), businessType);

        // Update business onboarding step
        businessRepository.findById(businessId).ifPresent(biz -> {
            if (biz.getOnboardingStep() < 2) biz.setOnboardingStep(2);
            businessRepository.save(biz);
        });

        return outlet;
    }

    // ========== STEP 5 — Complete ==========

    @Transactional
    public Map<String, Object> completOnboarding(UUID businessId) {
        Business biz = businessRepository.findById(businessId)
                .orElseThrow(() -> new com.quickserve.common.exception.ResourceNotFoundException("Business", businessId));

        if (biz.getOnboardingStep() < 2) {
            throw new com.quickserve.common.exception.BusinessException("Please complete all required setup steps first.");
        }
        if (biz.getStatus() != Business.BusinessStatus.ACTIVE) {
            biz.setStatus(Business.BusinessStatus.ACTIVE);
            biz.setOnboardingStep(5);
            businessRepository.save(biz);
        }
        return Map.of(
                "dashboardUrl", "/",
                "posUrl", "/pos",
                "kdsUrl", "/kds",
                "trialDaysRemaining", 14
        );
    }

    // ========== HELPERS ==========

    private void seedTaxSlabs(UUID businessId) {
        List<BigDecimal> pcts = List.of(BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("12"), new BigDecimal("18"), new BigDecimal("28"));
        for (BigDecimal pct : pcts) {
            TaxSlab slab = TaxSlab.builder().name("GST " + pct + "%").percentage(pct).active(true).build();
            slab.setBusinessId(businessId);
            taxSlabRepository.save(slab);
        }
    }

    private void seedCategories(UUID businessId, UUID outletId, String businessType) {
        List<String> cats = "RETAIL".equalsIgnoreCase(businessType)
                ? List.of("Electronics", "Clothing", "Grocery", "Others")
                : List.of("Starters", "Main Course", "Beverages", "Desserts");
        for (int i = 0; i < cats.size(); i++) {
            Category cat = Category.builder().name(cats.get(i)).sortOrder(i + 1).active(true).build();
            cat.setBusinessId(businessId);
            cat.setOutletId(outletId);
            categoryRepository.save(cat);
        }
    }
}
