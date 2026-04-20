package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.dto.req.branch.CreateBranchRequest;
import com.example.bezma.dto.req.branch.UpdateBranchRequest;
import com.example.bezma.dto.res.branch.BranchResponse;
import com.example.bezma.service.iService.IBranchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Tag(name = "Branch Management", description = "Các API quản lý chi nhánh")
public class BranchController {

    private final IBranchService branchService;

    @Operation(summary = "Tạo chi nhánh mới (Admin)")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BranchResponse> createBranch(
            @RequestAttribute("tenantId") Long tenantId,
            @RequestBody @Valid CreateBranchRequest request) {
        return ApiResponse.<BranchResponse>builder()
                .data(branchService.createBranch(tenantId, request))
                .message("Tạo chi nhánh thành công!")
                .build();
    }

    @Operation(summary = "Cập nhật chi nhánh (Admin)")
    @PutMapping("/{branchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<BranchResponse> updateBranch(
            @RequestAttribute("tenantId") Long tenantId,
            @PathVariable Long branchId,
            @RequestBody @Valid UpdateBranchRequest request) {
        return ApiResponse.<BranchResponse>builder()
                .data(branchService.updateBranch(tenantId, branchId, request))
                .message("Cập nhật chi nhánh thành công!")
                .build();
    }

    @Operation(summary = "Lấy chi tiết chi nhánh")
    @GetMapping("/{branchId}")
    public ApiResponse<BranchResponse> getBranchDetail(
            @RequestAttribute("tenantId") Long tenantId,
            @PathVariable Long branchId) {
        return ApiResponse.<BranchResponse>builder()
                .data(branchService.getBranchDetail(tenantId, branchId))
                .build();
    }

    @Operation(summary = "Lấy danh sách tất cả chi nhánh")
    @GetMapping
    public ApiResponse<List<BranchResponse>> getAllBranches(
            @RequestAttribute("tenantId") Long tenantId) {
        return ApiResponse.<List<BranchResponse>>builder()
                .data(branchService.getBranchesByTenant(tenantId))
                .build();
    }

    @Operation(summary = "Lấy danh sách chi nhánh phân trang")
    @GetMapping("/list/paginated")
    public ApiResponse<Page<BranchResponse>> getBranchesPaginated(
            @RequestAttribute("tenantId") Long tenantId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "branchName") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return ApiResponse.<Page<BranchResponse>>builder()
                .data(branchService.getBranchesPaginated(tenantId, pageable))
                .build();
    }

    @Operation(summary = "Xóa chi nhánh (Admin)")
    @DeleteMapping("/{branchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteBranch(
            @RequestAttribute("tenantId") Long tenantId,
            @PathVariable Long branchId) {
        branchService.deleteBranch(tenantId, branchId);
        return ApiResponse.<Void>builder()
                .message("Xóa chi nhánh thành công!")
                .build();
    }

    @Operation(summary = "Lấy danh sách chi nhánh do tôi quản lý")
    @GetMapping("/my-branches")
    public ApiResponse<List<BranchResponse>> getMyBranches(
            @RequestAttribute("userId") Long userId) {
        return ApiResponse.<List<BranchResponse>>builder()
                .data(branchService.getBranchesByManager(userId))
                .build();
    }

}
