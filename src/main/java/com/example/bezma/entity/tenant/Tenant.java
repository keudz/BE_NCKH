package com.example.bezma.entity.tenant;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import com.example.bezma.common.base.BaseEntity;
import com.example.bezma.entity.user.User;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Entity
@Table(name = "tenants", indexes = {
        @Index(name = "idx_tenant_slug", columnList = "slug"),
        @Index(name = "idx_tenant_code", columnList = "tenant_code")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Tenant extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "tenant_code", unique = true, nullable = false, length = 50)
    private String tenantCode;

    @Column(name = "slug", unique = true, nullable = false, length = 100)
    private String slug; // Phục vụ SEO
    @Column(name = "phone", nullable = false, length = 10)
    private String phone;

    @Column(name = "domain", unique = true, length = 100)
    private String domain;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "logo_url")
    private String logo;

    @Column(name = "is_active")
    @Builder.Default
    private boolean active = true;
    @Enumerated(EnumType.STRING)
    @Column(name = "plan_type", length = 20)
    private PlanType planType;

    // --- Verification Flow ---
    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_confirm", length = 30)
    @Builder.Default
    private RegistrationStatus statusConfirm = RegistrationStatus.PENDING_VERIFICATION;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "working_start_time")
    @Builder.Default
    private LocalTime workingStartTime = LocalTime.of(8, 30); // Giờ mặc định là 8:30 sáng

    // --- Relationships ---
    @OneToMany(mappedBy = "tenant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<User> users;
}
