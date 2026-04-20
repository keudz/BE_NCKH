package com.example.bezma.dto.res.branch;

import com.example.bezma.dto.res.user.UserBasicResponse;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchResponse {

    private Long id;

    private String branchCode;

    private String branchName;

    private String address;

    private String phone;

    private String email;

    private String description;

    private boolean active;

    private UserBasicResponse manager;

    private Long employeeCount;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
