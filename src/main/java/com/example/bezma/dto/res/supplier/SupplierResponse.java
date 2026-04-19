package com.example.bezma.dto.res.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierResponse {
    private Long id;
    private String code;
    private String name;
    private String logo;
    private String logoColor;
    private String contactName;
    private String contactTitle;
    private String phone;
    private String address;
    private String danhMuc;
    private String trangThai;
    private Long tenantId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
