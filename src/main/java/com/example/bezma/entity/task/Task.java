package com.example.bezma.entity.task;

import com.example.bezma.entity.tenant.Tenant;
import com.example.bezma.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.Filter;

@Entity
@Table(name = "system_tasks", indexes = {
    @Index(name = "idx_task_tenant_status", columnList = "tenant_id, status"),
    @Index(name = "idx_task_tenant_created", columnList = "tenant_id, created_at"),
    @Index(name = "idx_task_assignee", columnList = "assignee_id")
})
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 20)
    private String priority; // HIGH, MEDIUM, LOW

    @Column(length = 50)
    private String category; // MARKETING, TECHNICAL, DESIGN, etc.

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TaskStatus status;

    @Column(name = "require_photo")
    @Builder.Default
    private Boolean requirePhoto = false; // Mặc định: KHÔNG bắt buộc

    private LocalDateTime dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private User assignee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "report_images", columnDefinition = "TEXT")
    private String reportImages;

    // ── CHECK-IN: Bắt đầu thực hiện công việc tại hiện trường ──

    @Column(name = "check_in_time")
    private LocalDateTime checkInTime;

    @Column(name = "check_in_latitude", precision = 10, scale = 7)
    private BigDecimal checkInLatitude;

    @Column(name = "check_in_longitude", precision = 10, scale = 7)
    private BigDecimal checkInLongitude;

    @Column(name = "check_in_photo", length = 500)
    private String checkInPhoto; // URL ảnh hiện trường khi bắt đầu

    @Column(name = "check_in_note", columnDefinition = "TEXT")
    private String checkInNote; // Ghi chú ngắn khi bắt đầu

    // ── COMPLETION: Hoàn thành công việc ──

    @Column(name = "completion_photo", length = 500)
    private String completionPhoto; // URL ảnh khi hoàn thành

    @Column(name = "completion_time")
    private LocalDateTime completionTime;

    @Column(name = "result_note", columnDefinition = "TEXT")
    private String resultNote; // Ghi chú kết quả công việc

    @Column(name = "review_note", columnDefinition = "TEXT")
    private String reviewNote; // Ghi chú của Admin khi duyệt/từ chối

    @Column(name = "reviewed_by")
    private Long reviewedBy; // ID của Admin đã duyệt

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt; // Thời điểm duyệt

    @Column(name = "customer_confirmed")
    @Builder.Default
    private Boolean customerConfirmed = false; // Xác nhận của khách hàng

    // ── Project ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private com.example.bezma.entity.project.Project project;

    // ── Customer Info ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id")
    private com.example.bezma.entity.customer.Customer customer;

    @Column(name = "customer_name")
    private String customerName;

    @Column(name = "company_name")
    private String companyName;

    @Column(name = "address")
    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "estimated_price")
    private BigDecimal estimatedPrice;

    // ── Audit fields ──

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) status = TaskStatus.TO_DO;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
