package com.example.bezma.dto.req.attendance;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateAttendanceRequest {
    private String status; // ON_TIME, LATE, CHECK_OUT, EARLY_LEAVE
    private LocalDateTime checkTime;
    private String note;
}
