package com.example.bezma.service.impl;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.branch.CreateBranchRequest;
import com.example.bezma.dto.req.branch.UpdateBranchRequest;
import com.example.bezma.dto.res.branch.BranchResponse;
import com.example.bezma.dto.res.user.UserBasicResponse;
import com.example.bezma.entity.branch.Branch;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.BranchRepository;
import com.example.bezma.repository.TenantRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IBranchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BranchServiceImpl implements IBranchService {

    private final BranchRepository branchRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

    @Override
    public BranchResponse createBranch(Long tenantId, CreateBranchRequest request) {
        // Validate tenant exists
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.TENANT_NOT_FOUND));

        // Check branch code already exists
        if (branchRepository.existsByBranchCodeAndTenant(request.getBranchCode(), tenantId)) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        // Get manager
        User manager = userRepository.findById(request.getManagerId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        // Create branch
        Branch branch = Branch.builder()
                .branchCode(request.getBranchCode())
                .branchName(request.getBranchName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .description(request.getDescription())
                .tenant(tenant)
                .manager(manager)
                .active(true)
                .build();

        Branch savedBranch = branchRepository.save(branch);
        log.info("Created new branch: {} for tenant: {}", savedBranch.getId(), tenantId);

        return mapToResponse(savedBranch, 0L);
    }

    @Override
    public BranchResponse updateBranch(Long tenantId, Long branchId, UpdateBranchRequest request) {
        Branch branch = branchRepository.findByIdAndTenant(branchId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));

        if (request.getBranchName() != null) {
            branch.setBranchName(request.getBranchName());
        }
        if (request.getAddress() != null) {
            branch.setAddress(request.getAddress());
        }
        if (request.getPhone() != null) {
            branch.setPhone(request.getPhone());
        }
        if (request.getEmail() != null) {
            branch.setEmail(request.getEmail());
        }
        if (request.getDescription() != null) {
            branch.setDescription(request.getDescription());
        }
        if (request.getManagerId() != null) {
            User manager = userRepository.findById(request.getManagerId())
                    .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
            branch.setManager(manager);
        }
        if (request.getActive() != null) {
            branch.setActive(request.getActive());
        }

        Branch updatedBranch = branchRepository.save(branch);
        log.info("Updated branch: {} for tenant: {}", branchId, tenantId);

        return mapToResponse(updatedBranch, 0L);
    }

    @Override
    public BranchResponse getBranchDetail(Long tenantId, Long branchId) {
        Branch branch = branchRepository.findByIdAndTenant(branchId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));

        long employeeCount = userRepository.countByBranchId(branchId);
        return mapToResponse(branch, employeeCount);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByTenant(Long tenantId) {
        List<Branch> branches = branchRepository.findAllByTenant(tenantId);
        return branches.stream()
                .map(branch -> {
                    long employeeCount = userRepository.countByBranchId(branch.getId());
                    return mapToResponse(branch, employeeCount);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponse> getBranchesPaginated(Long tenantId, Pageable pageable) {
        Page<Branch> branches = branchRepository.findAllByTenant(tenantId, pageable);
        return branches.map(branch -> {
            long employeeCount = userRepository.countByBranchId(branch.getId());
            return mapToResponse(branch, employeeCount);
        });
    }

    @Override
    public void deleteBranch(Long tenantId, Long branchId) {
        Branch branch = branchRepository.findByIdAndTenant(branchId, tenantId)
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_INPUT));

        branch.setActive(false);
        branchRepository.save(branch);
        log.info("Deleted branch: {} for tenant: {}", branchId, tenantId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getBranchesByManager(Long managerId) {
        List<Branch> branches = branchRepository.findByManager(managerId);
        return branches.stream()
                .map(branch -> {
                    long employeeCount = userRepository.countByBranchId(branch.getId());
                    return mapToResponse(branch, employeeCount);
                })
                .collect(Collectors.toList());
    }

    private BranchResponse mapToResponse(Branch branch, Long employeeCount) {
        return BranchResponse.builder()
                .id(branch.getId())
                .branchCode(branch.getBranchCode())
                .branchName(branch.getBranchName())
                .address(branch.getAddress())
                .phone(branch.getPhone())
                .email(branch.getEmail())
                .description(branch.getDescription())
                .active(branch.isActive())
                .manager(branch.getManager() != null ? UserBasicResponse.builder()
                        .id(branch.getManager().getId())
                        .username(branch.getManager().getUsername())
                        .fullName(branch.getManager().getFullName())
                        .avatar(branch.getManager().getAvatar())
                        .build() : null)
                .employeeCount(employeeCount)
                .createdAt(branch.getCreatedAt())
                .updatedAt(branch.getUpdatedAt())
                .build();
    }
}
