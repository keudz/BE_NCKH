package com.example.bezma.service.impl;

import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.attendance.AttendanceStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.common.enumCom.ErrorCode;
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
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

        // 0. Kiểm tra xem người dùng đã đăng ký chưa
        if (Boolean.TRUE.equals(user.getIsFaceRegistered())) {
            throw new AppException(ErrorCode.FACE_ALREADY_REGISTERED);
        }

        if (photos == null || photos.length == 0) {
            throw new AppException(ErrorCode.INVALID_INPUT);
        }

        try {
            // 1. Gửi danh sách ảnh sang AI Service để lấy Embedding trung bình
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

                // 2. Lưu Embedding vào MySQL dưới dạng JSON String
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
        } catch (Exception e) {
            log.error("AI Service Error: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    public Attendance checkIn(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon) {
        // 1. Load user (READ-ONLY, không cần transaction)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsFaceRegistered() || user.getFaceEmbedding() == null) {
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        }

        Tenant tenant = user.getTenant();

        // 2. GEOFENCING: Kiểm tra vị trí TRƯỚC khi gọi AI (tiết kiệm tài nguyên)
        validateLocation(tenant, lat, lon);

        // 3. Lưu ảnh bằng chứng (I/O thuần, không cần transaction)
        String photoUrl = saveEvidencePhoto("checkin", userId, photo);

        // 4. Gọi AI Service để verify khuôn mặt (HTTP call, KHÔNG nằm trong
        // transaction)
        boolean faceVerified = verifyFaceWithAI(photo, user.getFaceEmbedding());

        // 5. Xác định trạng thái điểm danh
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

        // 6. CHỈ phần save mới cần transaction (ngắn gọn, nhanh)
        return saveAttendanceRecord(user, tenant, lat, lon, photoUrl, status, note);
    }

    @Override
    public Attendance checkOut(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon) {
        // 1. Load user (READ-ONLY)
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsFaceRegistered() || user.getFaceEmbedding() == null) {
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        }

        Tenant tenant = user.getTenant();

        // 2. GEOFENCING: Kiểm tra vị trí
        validateLocation(tenant, lat, lon);

        // 3. Lưu ảnh bằng chứng
        String photoUrl = saveEvidencePhoto("checkout", userId, photo);

        // 4. Gọi AI Service (NGOÀI transaction)
        boolean faceVerified = verifyFaceWithAI(photo, user.getFaceEmbedding());

        // 5. Xác định trạng thái
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

        // 6. Save (trong transaction ngắn)
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
    // PRIVATE HELPER METHODS
    // ==========================================

    /**
     * Kiểm tra vị trí GPS của nhân viên có nằm trong bán kính cho phép không.
     * Nếu Tenant chưa cấu hình tọa độ văn phòng → bỏ qua (cho phép điểm danh ở bất
     * kỳ đâu).
     */
    private void validateLocation(Tenant tenant, BigDecimal lat, BigDecimal lon) {
        if (tenant.getOfficeLatitude() == null || tenant.getOfficeLongitude() == null) {
            log.warn("Tenant {} chưa cấu hình tọa độ văn phòng, bỏ qua kiểm tra vị trí.", tenant.getId());
            return; // Chưa cấu hình → skip, không block user
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

    /**
     * Lưu ảnh bằng chứng điểm danh vào thư mục uploads/.
     * Trả về đường dẫn tương đối của file ảnh.
     */
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

    /**
     * Gọi AI Service để verify khuôn mặt.
     * Method này KHÔNG có @Transactional → không giữ DB connection trong khi chờ
     * HTTP.
     *
     * @return true nếu khuôn mặt khớp, false nếu không khớp hoặc lỗi
     */
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
        } catch (Exception e) {
            log.error("Lỗi gọi AI Service: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Lưu bản ghi Attendance vào DB. Chỉ method này cần @Transactional.
     * Transaction rất ngắn (chỉ 1 lệnh INSERT), không giữ connection lâu.
     */
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
