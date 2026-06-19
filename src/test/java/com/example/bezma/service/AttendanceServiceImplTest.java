package com.example.bezma.service;

import com.example.bezma.common.enumCom.ErrorCode;
import com.example.bezma.dto.req.attendance.ManualAttendanceRequest;
import com.example.bezma.dto.req.attendance.UpdateAttendanceRequest;
import com.example.bezma.dto.res.attendance.AttendanceReportResponse;
import com.example.bezma.dto.res.attendance.AttendanceStatsResponse;
import com.example.bezma.entity.attendance.Attendance;
import com.example.bezma.entity.attendance.AttendanceStatus;
import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import com.example.bezma.exception.AppException;
import com.example.bezma.repository.AttendanceRepository;
import com.example.bezma.repository.UserRepository;
import com.example.bezma.service.CloudinaryService;
import com.example.bezma.service.impl.AttendanceServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceServiceImpl Unit Tests")
class AttendanceServiceImplTest {

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private UserRepository userRepository;
    @Mock private RestTemplate restTemplate;
    @Mock private ObjectMapper objectMapper;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private AttendanceServiceImpl attendanceService;

    private User testUser;
    private Tenant testTenant;
    private Attendance testAttendance;

    @BeforeEach
    void setUp() {
        testTenant = Tenant.builder()
                .id(1L)
                .name("Test Company")
                .workingStartTime(LocalTime.of(8, 0))
                .workingEndTime(LocalTime.of(17, 30))
                .officeLatitude(new BigDecimal("10.8230989"))
                .officeLongitude(new BigDecimal("106.6296638"))
                .allowedRadius(200.0)
                .build();

        testUser = User.builder()
                .id(1L)
                .username("employee1")
                .fullName("John Doe")
                .email("john@example.com")
                .tenant(testTenant)
                .isFaceRegistered(true)
                .faceEmbedding("[0.1, 0.2]")
                .isActive(true)
                .build();

        testAttendance = Attendance.builder()
                .id(100L)
                .user(testUser)
                .tenant(testTenant)
                .checkTime(LocalDateTime.now())
                .status(AttendanceStatus.ON_TIME)
                .build();
    }

    @Nested
    @DisplayName("Check-In Tests")
    class CheckInTests {

        @Test
        @DisplayName("Check-in thành công đúng giờ")
        void checkIn_Success_OnTime() throws Exception {
            MultipartFile mockPhoto = mock(MultipartFile.class);
            when(mockPhoto.getBytes()).thenReturn(new byte[]{1, 2, 3});
            BigDecimal lat = new BigDecimal("10.8230989");
            BigDecimal lon = new BigDecimal("106.6296638");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), anyList(), any(), any()))
                    .thenReturn(false);
            when(cloudinaryService.uploadFile(any(), anyString())).thenReturn("http://cloudinary.com/photo.jpg");

            Map<String, Object> aiResponse = new HashMap<>();
            aiResponse.put("verified", true);
            ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(aiResponse, HttpStatus.OK);
            when(restTemplate.exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(ParameterizedTypeReference.class)))
                    .thenReturn(responseEntity);

            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

            testTenant.setWorkingStartTime(LocalTime.now().plusHours(2));
            Attendance result = attendanceService.checkIn(1L, mockPhoto, lat, lon);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ON_TIME);
            verify(attendanceRepository).save(any());
        }

        @Test
        @DisplayName("Check-in trùng lặp trong ngày → throw ALREADY_CHECKED_IN")
        void checkIn_Duplicate_ThrowsAppException() {
            MultipartFile mockPhoto = mock(MultipartFile.class);
            BigDecimal lat = new BigDecimal("10.8230989");
            BigDecimal lon = new BigDecimal("106.6296638");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), anyList(), any(), any()))
                    .thenReturn(true);

            AppException exception = catchThrowableOfType(
                    () -> attendanceService.checkIn(1L, mockPhoto, lat, lon), AppException.class);

            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CHECKED_IN);
        }

        @Test
        @DisplayName("Check-in ngoài vùng định vị → throw LOCATION_OUT_OF_RANGE")
        void checkIn_OutOfRange_ThrowsAppException() {
            MultipartFile mockPhoto = mock(MultipartFile.class);
            BigDecimal farLat = new BigDecimal("11.8230989");
            BigDecimal farLon = new BigDecimal("107.6296638");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), anyList(), any(), any()))
                    .thenReturn(false);

            AppException exception = catchThrowableOfType(
                    () -> attendanceService.checkIn(1L, mockPhoto, farLat, farLon), AppException.class);

            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.LOCATION_OUT_OF_RANGE);
        }
    }

    @Nested
    @DisplayName("Check-Out Tests")
    class CheckOutTests {

        @Test
        @DisplayName("Check-out trùng lặp → throw ALREADY_CHECKED_OUT")
        void checkOut_Duplicate_ThrowsAppException() {
            MultipartFile mockPhoto = mock(MultipartFile.class);
            BigDecimal lat = new BigDecimal("10.8230989");
            BigDecimal lon = new BigDecimal("106.6296638");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), eq(List.of(AttendanceStatus.CHECK_OUT, AttendanceStatus.EARLY_LEAVE)), any(), any()))
                    .thenReturn(true);

            AppException exception = catchThrowableOfType(
                    () -> attendanceService.checkOut(1L, mockPhoto, lat, lon), AppException.class);

            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ALREADY_CHECKED_OUT);
        }

        @Test
        @DisplayName("Check-out khi chưa check-in → throw NOT_CHECKED_IN_YET")
        void checkOut_NoCheckIn_ThrowsAppException() {
            MultipartFile mockPhoto = mock(MultipartFile.class);
            BigDecimal lat = new BigDecimal("10.8230989");
            BigDecimal lon = new BigDecimal("106.6296638");

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), eq(List.of(AttendanceStatus.CHECK_OUT, AttendanceStatus.EARLY_LEAVE)), any(), any()))
                    .thenReturn(false);
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), eq(List.of(AttendanceStatus.ON_TIME, AttendanceStatus.LATE, AttendanceStatus.SUCCESS)), any(), any()))
                    .thenReturn(false);

            AppException exception = catchThrowableOfType(
                    () -> attendanceService.checkOut(1L, mockPhoto, lat, lon), AppException.class);

            assertThat(exception).isNotNull();
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.NOT_CHECKED_IN_YET);
        }
    }

    @Nested
    @DisplayName("Admin Management Tests")
    class AdminManagementTests {

        @Test
        @DisplayName("Lấy danh sách điểm danh hôm nay")
        void getTodayAttendance_Success() {
            when(attendanceRepository.findByTenantIdAndCheckTimeBetweenOrderByCheckTimeDesc(eq(1L), any(), any()))
                    .thenReturn(List.of(testAttendance));

            List<Attendance> result = attendanceService.getTodayAttendance(1L);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getId()).isEqualTo(100L);
        }

        @Test
        @DisplayName("Lấy thống kê nhanh hôm nay")
        void getTodayStats_Success() {
            when(userRepository.findAllByTenantIdAndIsDeleted(1L, false)).thenReturn(List.of(testUser));
            when(attendanceRepository.findByTenantIdAndCheckTimeBetweenOrderByCheckTimeDesc(eq(1L), any(), any()))
                    .thenReturn(List.of(testAttendance));

            AttendanceStatsResponse stats = attendanceService.getTodayStats(1L);

            assertThat(stats).isNotNull();
            assertThat(stats.getTotalEmployees()).isEqualTo(1);
            assertThat(stats.getPresentCount()).isEqualTo(1);
            assertThat(stats.getAbsentCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("Điểm danh thủ công CHECK_IN")
        void createManualAttendance_CheckIn_Success() {
            ManualAttendanceRequest request = ManualAttendanceRequest.builder()
                    .userId(1L)
                    .type("CHECK_IN")
                    .status("ON_TIME")
                    .note("Lý do cá nhân")
                    .build();

            when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
            when(attendanceRepository.existsByUserIdAndStatusInAndCheckTimeBetween(eq(1L), anyList(), any(), any()))
                    .thenReturn(false);
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Attendance result = attendanceService.createManualAttendance(1L, request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AttendanceStatus.ON_TIME);
            assertThat(result.getNote()).contains("Lý do cá nhân");
        }

        @Test
        @DisplayName("Chỉnh sửa bản ghi điểm danh")
        void updateAttendance_Success() {
            UpdateAttendanceRequest request = UpdateAttendanceRequest.builder()
                    .status("LATE")
                    .note("Được duyệt đi muộn")
                    .build();

            when(attendanceRepository.findById(100L)).thenReturn(Optional.of(testAttendance));
            when(attendanceRepository.save(any(Attendance.class))).thenAnswer(invocation -> invocation.getArgument(0));

            Attendance result = attendanceService.updateAttendance(100L, request);

            assertThat(result).isNotNull();
            assertThat(result.getStatus()).isEqualTo(AttendanceStatus.LATE);
            assertThat(result.getNote()).contains("Được duyệt đi muộn");
        }

        @Test
        @DisplayName("Lấy báo cáo chấm công tháng")
        void getMonthlyReport_Success() {
            when(userRepository.findAllByTenantIdAndIsDeleted(1L, false)).thenReturn(List.of(testUser));
            when(attendanceRepository.getHistoryByUserAndMonth(eq(1L), eq(6), eq(2026)))
                    .thenReturn(List.of(testAttendance));

            List<AttendanceReportResponse> report = attendanceService.getMonthlyReport(1L, 6, 2026);

            assertThat(report).hasSize(1);
            assertThat(report.get(0).getFullName()).isEqualTo("John Doe");
            assertThat(report.get(0).getTotalWorkingDays()).isEqualTo(1);
            assertThat(report.get(0).getOnTimeCount()).isEqualTo(1);
        }
    }
}
