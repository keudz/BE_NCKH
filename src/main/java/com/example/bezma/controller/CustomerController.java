package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.entity.customer.Customer;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.CustomerRepository;
//import com.example.bezma.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import com.example.bezma.common.res.PageResponse;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

        private final CustomerRepository customerRepository;
        // private final TenantRepository tenantRepository;

        @GetMapping
        public ApiResponse<PageResponse<Customer>> getAll(
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @AuthenticationPrincipal User currentUser) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Customer> customerPage = customerRepository.findByTenantId(currentUser.getTenant().getId(), pageable);
                
                PageResponse<Customer> pageResponse = PageResponse.<Customer>builder()
                        .content(customerPage.getContent())
                        .pageNumber(customerPage.getNumber())
                        .pageSize(customerPage.getSize())
                        .totalElements(customerPage.getTotalElements())
                        .totalPages(customerPage.getTotalPages())
                        .last(customerPage.isLast())
                        .build();

                return ApiResponse.<PageResponse<Customer>>builder()
                                .data(pageResponse)
                                .build();
        }

        @GetMapping("/search")
        public ApiResponse<PageResponse<Customer>> search(
                        @RequestParam("query") String query,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        @AuthenticationPrincipal User currentUser) {
                Pageable pageable = PageRequest.of(page, size);
                Page<Customer> customerPage = customerRepository.searchCustomers(currentUser.getTenant().getId(), query, pageable);
                
                PageResponse<Customer> pageResponse = PageResponse.<Customer>builder()
                        .content(customerPage.getContent())
                        .pageNumber(customerPage.getNumber())
                        .pageSize(customerPage.getSize())
                        .totalElements(customerPage.getTotalElements())
                        .totalPages(customerPage.getTotalPages())
                        .last(customerPage.isLast())
                        .build();

                return ApiResponse.<PageResponse<Customer>>builder()
                                .data(pageResponse)
                                .build();
        }

        @PostMapping
        public ApiResponse<Customer> create(
                        @RequestBody Customer customer,
                        @AuthenticationPrincipal User currentUser) {
                customer.setTenant(currentUser.getTenant());
                return ApiResponse.<Customer>builder()
                                .data(customerRepository.save(customer))
                                .message("Tạo khách hàng thành công!")
                                .build();
        }

        @PutMapping("/{id}")
        public ApiResponse<Customer> update(
                        @PathVariable Long id,
                        @RequestBody Customer request,
                        @AuthenticationPrincipal User currentUser) {
                Customer customer = customerRepository.findById(id)
                                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng"));

                customer.setName(request.getName());
                customer.setPhoneNumber(request.getPhoneNumber());
                customer.setCompanyName(request.getCompanyName());
                customer.setAddress(request.getAddress());
                customer.setEmail(request.getEmail());
                customer.setTaxCode(request.getTaxCode());
                customer.setNote(request.getNote());

                return ApiResponse.<Customer>builder()
                                .data(customerRepository.save(customer))
                                .message("Cập nhật khách hàng thành công!")
                                .build();
        }

        @DeleteMapping("/{id}")
        public ApiResponse<Void> delete(@PathVariable Long id) {
                customerRepository.deleteById(id);
                return ApiResponse.<Void>builder()
                                .message("Xóa khách hàng thành công!")
                                .build();
        }
}
