package com.example.bezma.service.iService;

import com.example.bezma.dto.res.dashboard.DashboardSummaryResponse;

public interface IDashboardService {
    DashboardSummaryResponse getSummary(Long tenantId);
}
