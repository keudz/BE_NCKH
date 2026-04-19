package com.example.bezma.dto.req.supplier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupplierRequest {
    @NotBlank(message = "Mã nhà cung cấp không được để trống")
    private String code;
    
    @NotBlank(message = "Tên nhà cung cấp không được để trống")
    private String name;
    
    private String logo;
    private String logoColor;
    private String contactName;
    private String contactTitle;
    private String phone;
    private String address;
    private String danhMuc;
    private String trangThai;
}
