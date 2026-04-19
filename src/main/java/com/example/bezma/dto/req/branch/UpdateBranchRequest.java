package com.example.bezma.dto.req.branch;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateBranchRequest {

    private String branchName;

    private String address;

    private String phone;

    private String email;

    private String description;

    private Long managerId;

    private Boolean active;
}
