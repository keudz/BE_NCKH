package com.example.bezma.entity.attendance;

import com.example.bezma.common.base.BaseEntity;
import com.example.bezma.entity.user.User;
import com.example.bezma.entity.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Filter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances", indexes = {
    @Index(name = "idx_attendance_tenant", columnList = "tenant_id")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Attendance extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "check_time")
    private LocalDateTime checkTime;

    // Tọa độ GPS (Vĩ độ - Kinh độ)
    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    // Ảnh bằng chứng (chụp lúc chấm công) - lưu URL cloud hoặc local
    @Column(name = "photo_url")
    private String photoUrl;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private AttendanceStatus status;

    // Ghi chú nếu cần (ví dụ: "Muộn do kẹt xe")
    @Column(columnDefinition = "TEXT")
    private String note;
}
