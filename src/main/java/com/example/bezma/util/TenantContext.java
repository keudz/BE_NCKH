package com.example.bezma.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TenantContext {

    // ThreadLocal lưu trữ shopId
    private static final ThreadLocal<Long> currentTenantId = new ThreadLocal<>();

    public static void setCurrentTenantId(Long tenantId) {
        currentTenantId.set(tenantId);
    }

    /**
     * Lấy ID của shop hiện tại.
     * Trả về NULL nếu là Platform Admin (truy cập toàn sàn).
     */
    public static Long getCurrentTenantId() {
        return currentTenantId.get();
    }

    /**
     * Kiểm tra xem request hiện tại có bị giới hạn bởi Shop không.
     * Trả về true nếu là Tenant/User, false nếu là Platform Admin.
     */
    public static boolean isTenantMode() {
        return currentTenantId.get() != null;
    }

    public static void clear() {
        currentTenantId.remove();
    }
}