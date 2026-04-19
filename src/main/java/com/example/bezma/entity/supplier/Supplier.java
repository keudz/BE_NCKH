package com.example.bezma.entity.supplier;

import com.example.bezma.entity.tenant.Tenant;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code; // Mã NCC (VD: SUP-2024-001)

    @Column(nullable = false)
    private String name; // Tên NCC

    private String logo; // Ký hiệu logo (VD: TV)
    
    private String logoColor; // Màu logo (VD: #3B82F6)

    private String contactName; // Người liên hệ
    
    private String contactTitle; // Chức vụ
    
    private String phone; // Số điện thoại
    
    @Column(columnDefinition = "TEXT")
    private String address; // Địa chỉ
    
    @Column(name = "danh_muc")
    private String danhMuc; // Công nghệ, Vận tải, Hoá chất
    
    @Column(name = "trang_thai")
    private String trangThai; // HOẠT ĐỘNG, CHỜ DUYỆT, TẠM DỪNG

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
