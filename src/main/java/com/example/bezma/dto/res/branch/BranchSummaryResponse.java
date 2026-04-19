package com.example.bezma.dto.res.branch;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BranchSummaryResponse {

    private Long id;

    private String branchCode;

    private String branchName;

    private Long employeeCount;
}
