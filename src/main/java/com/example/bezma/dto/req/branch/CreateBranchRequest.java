package com.example.bezma.dto.req.branch;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateBranchRequest {

    @NotBlank(message = "Mã chi nhánh không được trống")
    private String branchCode;

    @NotBlank(message = "Tên chi nhánh không được trống")
    private String branchName;

    @NotBlank(message = "Địa chỉ không được trống")
    private String address;

    private String phone;

    private String email;

    private String description;

    @NotNull(message = "ID người quản lý không được trống")
    private Long managerId;
}
