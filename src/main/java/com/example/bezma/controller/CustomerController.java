package com.example.bezma.controller;

import com.example.bezma.common.res.ApiResponse;
import com.example.bezma.entity.customer.Customer;
import com.example.bezma.entity.user.User;
import com.example.bezma.repository.CustomerRepository;
//import com.example.bezma.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
public class CustomerController {

        private final CustomerRepository customerRepository;
        // private final TenantRepository tenantRepository;

        @GetMapping
        public ApiResponse<List<Customer>> getAll(@AuthenticationPrincipal User currentUser) {
                return ApiResponse.<List<Customer>>builder()
                                .data(customerRepository.findByTenantId(currentUser.getTenant().getId()))
                                .build();
        }

        @GetMapping("/search")
        public ApiResponse<List<Customer>> search(
                        @RequestParam("query") String query,
                        @AuthenticationPrincipal User currentUser) {
                return ApiResponse.<List<Customer>>builder()
                                .data(customerRepository.searchCustomers(currentUser.getTenant().getId(), query))
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
