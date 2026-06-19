package com.example.bezma.dto.res.attendance;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceReportResponse {
    private Long userId;
    private String fullName;
    private String username;
    private String email;
    private long totalWorkingDays;
    private long onTimeCount;
    private long lateCount;
    private long earlyLeaveCount;
}
