package com.example.bezma.controller;

import com.example.bezma.dto.req.user.UserCreateRequest;
import com.example.bezma.dto.res.user.UserCreateResponse;
import com.example.bezma.service.iService.IAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AdminController {

    private final IAdminService adminService;

    @PostMapping("tenants/{tenantId}/users")
    public ResponseEntity<UserCreateResponse> createUser(@PathVariable("tenantId") Long tenantId,
                                                                  @RequestBody UserCreateRequest req) {

        UserCreateResponse response = adminService.createUser(req, tenantId);

        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

}
