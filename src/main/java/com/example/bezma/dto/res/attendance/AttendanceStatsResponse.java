package com.example.bezma.dto.res.attendance;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AttendanceStatsResponse {
    private long totalEmployees;
    private long presentCount;
    private long lateCount;
    private long earlyLeaveCount;
    private long absentCount;
}
