package com.example.bezma.service.iService;

import com.example.bezma.entity.attendance.Attendance;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.util.List;

public interface IAttendanceService {
    Attendance checkIn(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon);
    void registerFace(Long userId, MultipartFile photo);
    List<Attendance> getMyAttendance(Long userId);
}
