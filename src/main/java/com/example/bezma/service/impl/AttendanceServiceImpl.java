package com.example.bezma.service.impl;

import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.attendance.AttendanceStatus;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.repository.AttendanceRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.iService.IAttendanceService;
//import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    @Value("${ai.service.url}")
    private String aiServiceUrl;

    @Override
    @Transactional
    public void registerFace(Long userId, MultipartFile photo) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        try {
            // 1. Gửi ảnh sang AI Service để lấy Embedding
            String url = aiServiceUrl + "/extract-embedding";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            });

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

                log.info("Face registered successfully for user: {}", userId);
            }
        } catch (IOException e) {
            log.error("Lỗi đọc file ảnh: {}", e.getMessage());
            throw new AppException(ErrorCode.INVALID_INPUT);
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("AI Service - Không thấy mặt: {}", e.getMessage());
            throw new AppException(ErrorCode.FACE_NOT_DETECTED);
        } catch (Exception e) {
            log.error("AI Service Error: {}", e.getMessage());
            throw new AppException(ErrorCode.AI_SERVICE_ERROR);
        }
    }

    @Override
    @Transactional
    public Attendance checkIn(Long userId, MultipartFile photo, BigDecimal lat, BigDecimal lon) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (!user.getIsFaceRegistered() || user.getFaceEmbedding() == null) {
            throw new RuntimeException("Người dùng chưa đăng ký khuôn mặt!");
        }

        Attendance attendance = Attendance.builder()
                .user(user)
                .checkTime(LocalDateTime.now())
                .latitude(lat)
                .longitude(lon)
                .status(AttendanceStatus.PENDING)
                .build();

        try {
            String url = aiServiceUrl + "/verify-with-embedding";

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("image", new ByteArrayResource(photo.getBytes()) {
                @Override
                public String getFilename() {
                    return photo.getOriginalFilename();
                }
            });
            body.add("stored_embedding_json", user.getFaceEmbedding());

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
                if (Boolean.TRUE.equals(verified)) {
                    attendance.setStatus(AttendanceStatus.SUCCESS);
                } else {
                    attendance.setStatus(AttendanceStatus.FAIL_FACE);
                }
            }
        } catch (HttpClientErrorException.BadRequest e) {
            log.error("AI Verification - Không thấy mặt: {}", e.getMessage());
            attendance.setStatus(AttendanceStatus.FAIL_FACE);
            attendance.setNote(ErrorCode.FACE_NOT_DETECTED.getMessage());
        } catch (Exception e) {
            log.error("AI Verification Error: {}", e.getMessage());
            attendance.setStatus(AttendanceStatus.FAIL_FACE);
            attendance.setNote("Lỗi hệ thống nhận diện: " + e.getMessage());
        }

        return attendanceRepository.save(attendance);
    }

    @Override
    public List<Attendance> getMyAttendance(Long userId) {
        return attendanceRepository.findAllByUserIdOrderByCheckTimeDesc(userId);
    }
}
