package com.quickserve.common.security;

import java.util.UUID;

/**
 * Thread-local holder for the current tenant's businessId.
 * Populated by JwtAuthFilter on every authenticated request.
 * Cleared in the finally block of the filter.
 */
public final class TenantContext {

    private static final ThreadLocal<UUID> CURRENT_BUSINESS_ID = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_OUTLET_ID   = new ThreadLocal<>();
    private static final ThreadLocal<UUID> CURRENT_USER_ID     = new ThreadLocal<>();

    private TenantContext() {}

    public static void setBusinessId(UUID businessId) {
        CURRENT_BUSINESS_ID.set(businessId);
    }

    public static UUID getBusinessId() {
        return CURRENT_BUSINESS_ID.get();
    }

    public static void setOutletId(UUID outletId) {
        CURRENT_OUTLET_ID.set(outletId);
    }

    public static UUID getOutletId() {
        return CURRENT_OUTLET_ID.get();
    }

    public static void setUserId(UUID userId) {
        CURRENT_USER_ID.set(userId);
    }

    public static UUID getUserId() {
        return CURRENT_USER_ID.get();
    }

    public static void clear() {
        CURRENT_BUSINESS_ID.remove();
        CURRENT_OUTLET_ID.remove();
        CURRENT_USER_ID.remove();
    }
}
