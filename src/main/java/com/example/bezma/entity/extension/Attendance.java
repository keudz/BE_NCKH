package com.example.bezma.entity.extension;

import com.example.bezma.common.base.BaseEntity;
import com.example.bezma.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendances")
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

