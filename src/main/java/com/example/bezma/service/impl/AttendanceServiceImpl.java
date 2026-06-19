package com.example.bezma.service.impl;

import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.attendance.AttendanceStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.attendance.ManualAttendanceRequest;
import com.example.bezma.dto.req.attendance.UpdateAttendanceRequest;
import com.example.bezma.dto.res.attendance.AttendanceReportResponse;
import com.example.bezma.dto.res.attendance.AttendanceStatsResponse;
import com.example.bezma.repository.AttendanceRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IAttendanceService;
import com.example.bezma.util.GeoUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.example.bezma.service.CloudinaryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements IAttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final CloudinaryService cloudinaryService;

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Override
    @Transactional
    public void registerFace(Long userId, MultipartFile[] photos) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (Boolean.TRUE.equals(user.getIsFaceRegistered())) {
            throw new AppException(ErrorCode.FACE_ALREADY_REGISTERED);
        }

        if (photos == null || photos.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        try {
            String url = aiServiceUrl + "/extract-embeddings";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            for (MultipartFile photo : photos) {
                body.add("files", new ByteArrayResource(photo.getBytes()) {
                    @Override
                    public String getFilename() {
                        return photo.getOriginalFilename();
                    }
                });
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Double> embedding = objectMapper.convertValue(
                        response.getBody().get("embedding"),
                        new TypeReference<List<Double>>() {
                        });

                String embeddingJson = objectMapper.writeValueAsString(embedding);
                user.setFaceEmbedding(embeddingJson);
                user.setIsFaceRegistered(true);
                userRepository.save(user);

                log.info("Face registered successfully with multiple photos for user: {}", userId);
            }
        } catch (IOException e) {
            log.error("Lỗi đọc file ảnh: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_INPUT);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("AI Service - Lỗi xử lý: {}", e.getMessage());
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        } catch (ResourceAccessException e) {
            log.error("AI Service kết nối thất bại (timeout): {}", e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("AI Service Error: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Attendance checkIn(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsFaceRegistered() || user.getFaceEmbedding() == null) {
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        }

        // Chống check-in trùng lặp
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        boolean alreadyCheckedIn = attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(
                userId,
                List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS),
                startOfDay,
                endOfDay
        );
        if (alreadyCheckedIn) {
            throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
        }

        Tenant tenant = user.getTenant();
        validateLocation(tenant, lat, lon);

        String photoUrl = saveEvidencePhoto("checkin", userId, photo);
        boolean faceVerified = verifyFaceWithAI(photo, user.getFaceEmbedding());

        AttendanceStatus status;
        String note = null;

        if (faceVerified) {
            LocalTime now = LocalTime.now();
            LocalTime startTime = tenant.getWorkingStartTime();
            if (startTime == null) {
                startTime = LocalTime.of(8, 0);
            }

            if (now.isAfter(startTime)) {
                status = AttendanceStatus.LATE;
                note = "Đi muộn (Vào lúc " + now + ")";
            } else {
                status = AttendanceStatus.ON_TIME;
            }
        } else {
            status = AttendanceStatus.FAIL_FACE;
            note = "Khuôn mặt không khớp với dữ liệu đã đăng ký";
        }

        return saveAttendanceRecord(user, tenant, lat, lon, photoUrl, status, note);
    }

    @Override
    public Attendance checkOut(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsFaceRegistered() || user.getFaceEmbedding() == null) {
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        }

        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);

        // Chống check-out trùng lặp
        boolean alreadyCheckedOut = attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(
                userId,
                List.of(AttendanceStatus.CHECK_OUT, AttendanceStatus.EARLY_LEAVE),
                startOfDay,
                endOfDay
        );
        if (alreadyCheckedOut) {
            throw new AppException(ErrorCode.ALREADY_CHECKED_OUT);
        }

        // Yêu cầu phải check-in trước
        boolean hasCheckedIn = attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(
                userId,
                List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS),
                startOfDay,
                endOfDay
        );
        if (!hasCheckedIn) {
            throw new AppException(ErrorCode.NOT_CHECKED_IN_YET);
        }

        Tenant tenant = user.getTenant();
        validateLocation(tenant, lat, lon);

        String photoUrl = saveEvidencePhoto("checkout", userId, photo);
        boolean faceVerified = verifyFaceWithAI(photo, user.getFaceEmbedding());

        AttendanceStatus status;
        String note = null;

        if (faceVerified) {
            LocalTime now = LocalTime.now();
            LocalTime endTime = tenant.getWorkingEndTime();
            if (endTime == null) {
                endTime = LocalTime.of(17, 30);
            }

            if (now.isBefore(endTime)) {
                status = AttendanceStatus.EARLY_LEAVE;
                note = "Về sớm (Về lúc " + now + ")";
            } else {
                status = AttendanceStatus.CHECK_OUT;
                note = "Về đúng giờ";
            }
        } else {
            status = AttendanceStatus.FAIL_FACE;
            note = "Khuôn mặt không khớp với dữ liệu đã đăng ký";
        }

        return saveAttendanceRecord(user, tenant, lat, lon, photoUrl, status, note);
    }

    @Override
    public List<Attendance> getMyAttendance(Long userId) {
        return attendanceRepository.findAllByUserIdOrderByCheckTimeDesc(userId);
    }

    @Override
    public List<Attendance> getHistoryByMonth(Long userId, int month, int year) {
        return attendanceRepository.getHistoryByUserAndMonth(userId, month, year);
    }

    @Override
    public List<Attendance> getTenantHistoryByMonth(Long tenantId, int month, int year) {
        return attendanceRepository.getTenantHistoryByMonth(tenantId, month, year);
    }

    // ==========================================
    // ADMIN PHASE 2 METHODS
    // ==========================================

    @Override
    public List<Attendance> getTodayAttendance(Long tenantId) {
        LocalDateTime startOfDay = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.now().with(LocalTime.MAX);
        return attendanceRepository.findByTenantIdAndCheckTimeBetweenOrderByCheckTimeDesc(tenantId, startOfDay, endOfDay);
    }

    @Override
    public AttendanceStatsResponse getTodayStats(Long tenantId) {
        List<User> activeEmployees = userRepository.findAllByTenantIdAndIsDeleted(tenantId, false);
        long totalEmployees = activeEmployees.size();

        List<Attendance> todayRecords = getTodayAttendance(tenantId);

        long presentCount = todayRecords.stream()
                .filter(r -> List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS).contains(r.getStatus()))
                .map(r -> r.getUser().getId())
                .distinct()
                .count();

        long lateCount = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.LATE)
                .map(r -> r.getUser().getId())
                .distinct()
                .count();

        long earlyLeaveCount = todayRecords.stream()
                .filter(r -> r.getStatus() == AttendanceStatus.EARLY_LEAVE)
                .map(r -> r.getUser().getId())
                .distinct()
                .count();

        long absentCount = Math.max(0, totalEmployees - presentCount);

        return AttendanceStatsResponse.builder()
                .totalEmployees(totalEmployees)
                .presentCount(presentCount)
                .lateCount(lateCount)
                .earlyLeaveCount(earlyLeaveCount)
                .absentCount(absentCount)
                .build();
    }

    @Override
    @Transactional
    public Attendance createManualAttendance(Long tenantId, ManualAttendanceRequest request) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getTenant().getId().equals(tenantId)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        LocalDateTime checkTime = request.getCheckTime() != null ? request.getCheckTime() : LocalDateTime.now();
        LocalDateTime startOfDay = checkTime.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = startOfDay.plusDays(1).minusNanos(1);

        AttendanceStatus status;
        String note = request.getNote();
        if (note == null || note.isEmpty()) {
            note = "Admin chấm công thủ công";
        } else {
            note = "Admin chấm công thủ công: " + note;
        }

        if ("CHECK_IN".equalsIgnoreCase(request.getType())) {
            boolean alreadyCheckedIn = attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(
                    user.getId(),
                    List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS),
                    startOfDay,
                    endOfDay
            );
            if (alreadyCheckedIn) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_IN);
            }
            status = request.getStatus() != null ? AttendanceStatus.valueOf(request.getStatus()) : AttendanceStatus.ON_TIME;
        } else {
            boolean alreadyCheckedOut = attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(
                    user.getId(),
                    List.of(AttendanceStatus.CHECK_OUT, AttendanceStatus.EARLY_LEAVE),
                    startOfDay,
                    endOfDay
            );
            if (alreadyCheckedOut) {
                throw new AppException(ErrorCode.ALREADY_CHECKED_OUT);
            }
            status = request.getStatus() != null ? AttendanceStatus.valueOf(request.getStatus()) : AttendanceStatus.CHECK_OUT;
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .tenant(user.getTenant())
                .checkTime(checkTime)
                .status(status)
                .note(note)
                .build();

        return attendanceRepository.save(attendance);
    }

    @Override
    @Transactional
    public Attendance updateAttendance(Long id, UpdateAttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.ATTENDANCE_RECORD_NOT_FOUND));

        if (request.getStatus() != null) {
            attendance.setStatus(AttendanceStatus.valueOf(request.getStatus()));
        }
        if (request.getCheckTime() != null) {
            attendance.setCheckTime(request.getCheckTime());
        }
        
        String updatedNote = "Admin chỉnh sửa";
        if (request.getNote() != null && !request.getNote().isEmpty()) {
            updatedNote += ": " + request.getNote();
        }
        attendance.setNote(updatedNote);

        return attendanceRepository.save(attendance);
    }

    @Override
    public List<AttendanceReportResponse> getMonthlyReport(Long tenantId, int month, int year) {
        List<User> users = userRepository.findAllByTenantIdAndIsDeleted(tenantId, false);
        List<AttendanceReportResponse> reports = new ArrayList<>();

        for (User user : users) {
            List<Attendance> userRecords = attendanceRepository.getHistoryByUserAndMonth(user.getId(), month, year);

            long totalWorkingDays = userRecords.stream()
                    .filter(r -> List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS).contains(r.getStatus()))
                    .map(r -> r.getCheckTime().toLocalDate())
                    .distinct()
                    .count();

            long onTimeCount = userRecords.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.ON_TIME || r.getStatus() == AttendanceStatus.SUCCESS)
                    .count();

            long lateCount = userRecords.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.LATE)
                    .count();

            long earlyLeaveCount = userRecords.stream()
                    .filter(r -> r.getStatus() == AttendanceStatus.EARLY_LEAVE)
                    .count();

            reports.add(AttendanceReportResponse.builder()
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .username(user.getUsername())
                    .email(user.getEmail())
                    .totalWorkingDays(totalWorkingDays)
                    .onTimeCount(onTimeCount)
                    .lateCount(lateCount)
                    .earlyLeaveCount(earlyLeaveCount)
                    .build());
        }

        return reports;
    }

    // ==========================================
    // PRIVATE HELPER METHODS
    // ==========================================

    private void validateLocation(Tenant tenant, BigDecimal lat, BigDecimal lon) {
        if (tenant.getOfficeLatitude() == null || tenant.getOfficeLongitude() == null) {
            log.warn("Tenant {} chưa cấu hình tọa độ văn phòng, bỏ qua kiểm tra vị trí.", tenant.getId());
            return;
        }

        double allowedRadius = (tenant.getAllowedRadius() != null) ? tenant.getAllowedRadius() : 200.0;

        boolean withinRadius = GeoUtils.isWithinRadius(
                lat, lon,
                tenant.getOfficeLatitude(), tenant.getOfficeLongitude(),
                allowedRadius);

        if (!withinRadius) {
            double distance = GeoUtils.calculateDistance(
                    lat, lon,
                    tenant.getOfficeLatitude(), tenant.getOfficeLongitude());
            log.warn("User ngoài vùng cho phép. Khoảng cách: {}m, Bán kính: {}m",
                    Math.round(distance), allowedRadius);
            throw new AppException(ErrorCode.LOCATION_OUT_OF_RANGE);
        }
    }

    private String saveEvidencePhoto(String prefix, Long userId, MultipartFile photo) {
        if (photo == null || photo.isEmpty())
            return null;
        try {
            return cloudinaryService.uploadFile(photo, "attendance/" + prefix);
        } catch (Exception e) {
            log.error("Lỗi lưu ảnh bằng chứng lên Cloudinary: {}", e.getMessage());
            return null;
        }
    }

    private boolean verifyFaceWithAI(MultipartFile photo, String storedEmbedding) {
        try {
            String url = aiServiceUrl + "/verify-with-embedding";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            });
            body.add("stored_embedding_json", storedEmbedding);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    requestEntity,
                    new ParameterizedTypeReference<Map<String, Object>>() {
                    });

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                Boolean verified = (Boolean) response.getBody().get("verified");
                return Boolean.TRUE.equals(verified);
            }

            return false;
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("AI Verification - Không thấy mặt: {}", e.getMessage());
            return false;
        } catch (ResourceAccessException e) {
            log.error("AI Verification - Không thể kết nối AI Service (timeout): {}", e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_UNAVAILABLE);
        } catch (Exception e) {
            log.error("Lỗi gọi AI Service: {}", e.getMessage());
            return false;
        }
    }

    @Transactional
    protected Attendance saveAttendanceRecord(User user, Tenant tenant,
            BigDecimal lat, BigDecimal lon,
            String photoUrl,
            AttendanceStatus status, String note) {
        Attendance attendance = Attendance.builder()
                .user(user)
                .tenant(tenant)
                .checkTime(LocalDateTime.now())
                .latitude(lat)
                .longitude(lon)
                .photoUrl(photoUrl)
                .status(status)
                .note(note)
                .build();

        return attendanceRepository.save(attendance);
    }
}
