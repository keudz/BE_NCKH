package com.example.bezma.dto.req.attendance;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ManualAttendanceRequest {
    private Long userId;
    private String type; // CHECK_IN, CHECK_OUT
    private LocalDateTime checkTime;
    private String status; // ON_TIME, LATE, CHECK_OUT, EARLY_LEAVE
    private String note;
}
