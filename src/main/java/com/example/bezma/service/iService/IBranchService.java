package com.example.bezma.service.iService;

import com.example.bezma.dto.req.branch.CreateBranchRequest;
import com.example.bezma.dto.req.branch.UpdateBranchRequest;
import com.example.bezma.dto.res.branch.BranchResponse;
import com.example.bezma.dto.res.branch.BranchSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface IBranchService {

    BranchResponse createBranch(Long tenantId, CreateBranchRequest request);
    BranchResponse updateBranch(Long tenantId, Long branchId, UpdateBranchRequest request);
    BranchResponse getBranchDetail(Long tenantId, Long branchId);
    List<BranchResponse> getBranchesByTenant(Long tenantId);
    Page<BranchResponse> getBranchesPaginated(Long tenantId, Pageable pageable);
    void deleteBranch(Long tenantId, Long branchId);
    List<BranchResponse> getBranchesByManager(Long managerId);
}
