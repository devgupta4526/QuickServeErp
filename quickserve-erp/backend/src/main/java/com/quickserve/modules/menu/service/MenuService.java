package com.quickserve.modules.menu.service;

import com.quickserve.common.exception.BusinessException;
import com.quickserve.common.exception.ResourceNotFoundException;
import com.quickserve.common.exception.TenantAccessException;
import com.quickserve.common.response.PagedResponse;
import com.quickserve.common.security.TenantContext;
import com.quickserve.modules.menu.entity.Category;
import com.quickserve.modules.menu.entity.MenuItem;
import com.quickserve.modules.menu.entity.TaxSlab;
import com.quickserve.modules.menu.repository.CategoryRepository;
import com.quickserve.modules.menu.repository.MenuItemRepository;
import com.quickserve.modules.menu.repository.TaxSlabRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class MenuService {

    private final CategoryRepository  categoryRepository;
    private final MenuItemRepository  menuItemRepository;
    private final TaxSlabRepository   taxSlabRepository;

    // ========== CATEGORIES ==========

    @Transactional(readOnly = true)
    public List<Category> getCategories() {
        return categoryRepository.findByBusinessIdAndActiveTrueOrderBySortOrderAsc(TenantContext.getBusinessId());
    }

    @Transactional
    public Category createCategory(String name, String imageUrl, int sortOrder) {
        Category cat = Category.builder()
                .name(name)
                .imageUrl(imageUrl)
                .sortOrder(sortOrder)
                .active(true)
                .build();
        cat.setBusinessId(TenantContext.getBusinessId());
        cat.setOutletId(TenantContext.getOutletId());
        return categoryRepository.save(cat);
    }

    @Transactional
    public Category updateCategory(UUID id, String name, String imageUrl, int sortOrder) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        verifyTenant(cat.getBusinessId());
        cat.setName(name);
        if (imageUrl != null) cat.setImageUrl(imageUrl);
        cat.setSortOrder(sortOrder);
        return categoryRepository.save(cat);
    }

    @Transactional
    public void deleteCategory(UUID id) {
        Category cat = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        verifyTenant(cat.getBusinessId());
        cat.setActive(false);
        categoryRepository.save(cat);
    }

    // ========== MENU ITEMS ==========

    @Transactional(readOnly = true)
    public PagedResponse<MenuItem> getMenuItems(int page, int size) {
        var pageResult = menuItemRepository.findByBusinessIdAndArchivedFalse(
                TenantContext.getBusinessId(), PageRequest.of(page, size));
        return PagedResponse.from(pageResult);
    }

    @Transactional
    public MenuItem createMenuItem(MenuItem item) {
        item.setBusinessId(TenantContext.getBusinessId());
        item.setOutletId(TenantContext.getOutletId());
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem updateMenuItem(UUID id, MenuItem updates) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
        verifyTenant(item.getBusinessId());
        if (updates.getName() != null)        item.setName(updates.getName());
        if (updates.getDescription() != null) item.setDescription(updates.getDescription());
        if (updates.getBasePrice() != null)   item.setBasePrice(updates.getBasePrice());
        if (updates.getImageUrl() != null)    item.setImageUrl(updates.getImageUrl());
        if (updates.getCategoryId() != null)  item.setCategoryId(updates.getCategoryId());
        item.setVeg(updates.isVeg());
        return menuItemRepository.save(item);
    }

    @Transactional
    public MenuItem toggleAvailability(UUID id, boolean available) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("MenuItem", id));
        verifyTenant(item.getBusinessId());
        item.setAvailable(available);
        return menuItemRepository.save(item);
    }

    @Transactional(readOnly = true)
    public List<MenuItem> getPublicMenu(UUID outletId) {
        return menuItemRepository.findByBusinessIdAndAvailableTrueAndArchivedFalse(
                getBusinessIdFromOutlet(outletId));
    }

    @Transactional(readOnly = true)
    public List<TaxSlab> getTaxSlabs() {
        return taxSlabRepository.findByBusinessIdAndActiveTrue(TenantContext.getBusinessId());
    }

    // ========== HELPERS ==========

    private void verifyTenant(UUID resourceBusinessId) {
        if (!TenantContext.getBusinessId().equals(resourceBusinessId)) {
            throw new TenantAccessException();
        }
    }

    private UUID getBusinessIdFromOutlet(UUID outletId) {
        // In public context there is no tenant; return from outlet's business
        // This will be fetched via a lightweight query in a real implementation
        return outletId; // placeholder — properly resolved via OutletRepository
    }

    // ========== SEEDING ==========

    @Transactional
    public void seedDefaultTaxSlabs(UUID businessId) {
        List<BigDecimal> pcts = List.of(
                BigDecimal.ZERO,
                new BigDecimal("5"),
                new BigDecimal("12"),
                new BigDecimal("18"),
                new BigDecimal("28")
        );
        for (BigDecimal pct : pcts) {
            TaxSlab slab = TaxSlab.builder()
                    .name("GST " + pct + "%")
                    .percentage(pct)
                    .active(true)
                    .build();
            slab.setBusinessId(businessId);
            taxSlabRepository.save(slab);
        }
    }
}
