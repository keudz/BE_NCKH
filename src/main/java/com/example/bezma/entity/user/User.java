package com.example.bezma.entity.user;

import com.example.bezma.common.base.BaseEntity;
import com.example.bezma.entity.auth.Permission;
import com.example.bezma.entity.auth.Role;
import com.example.bezma.entity.branch.Branch;
import com.example.bezma.entity.tenant.Tenant;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_user_fullname", columnList = "full_name"),
        @Index(name = "idx_user_phone", columnList = "phone_number"),
        @Index(name = "idx_user_username", columnList = "user_name"),
        @Index(name = "idx_user_tenant", columnList = "tenant_id"),
        @Index(name = "idx_user_status", columnList = "status"),
        @Index(name = "idx_user_created", columnList = "created_at")
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class User extends BaseEntity implements UserDetails {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "code")
    private String code;

    @Column(name = "user_name", unique = true, nullable = false, length = 50)
    private String username;

    @JsonIgnore
    @Column(name = "password", nullable = false)
    private String password;

    @Column(name = "email", unique = true, length = 100)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phone;

    @Column(name = "full_name", length = 100)
    private String fullName;

    @Column(name = "avatar_url")
    private String avatar;

    @Column(name = "birthday")
    private LocalDateTime birthday;

    @Column(name = "gender", length = 10)
    private String gender;

    @Column(name = "address")
    private String address;

    @Column(name = "identity_card", length = 20)
    private String identityCard;

    // --- Quan hệ cốt lõi ---

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch; // Chi nhánh mà nhân viên thuộc về

    // --- Status & Security ---

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    @Builder.Default
    private UserStatus status = UserStatus.PENDING_ACTIVE;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = false;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;


    @Column(name = "verification_token")
    private String verificationToken;

    @Column(name = "verification_token_expiry")
    private LocalDateTime verificationTokenExpiry;
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_permissions", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "permission_id"))
    private Set<Permission> permissions;
    @Column(unique = true)
    private String zaloId;

    @Column(name = "face_embedding", columnDefinition = "LONGTEXT")
    private String faceEmbedding;

    @Column(name = "is_face_registered")
    @Builder.Default
    private Boolean isFaceRegistered = false;

    @Column(name = "must_change_password")
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Override
    @JsonIgnore
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Dùng Set để tự động loại bỏ các quyền trùng lặp
        Set<SimpleGrantedAuthority> authorities = new HashSet<>();

        // 1. Lấy quyền từ Role (role_permissions)
        if (this.role != null && this.role.getPermissions() != null) {
            this.role.getPermissions().forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
            // Thêm chính cái Role đó với prefix ROLE_ (Dùng cho hasRole trong
            // SecurityConfig)
            authorities.add(new SimpleGrantedAuthority("ROLE_" + this.role.getName()));
        }

        // 2. Lấy thêm quyền đặc cách được gán trực tiếp (user_permissions)
        if (this.permissions != null) {
            this.permissions.forEach(p -> authorities.add(new SimpleGrantedAuthority(p.getName())));
        }

        return authorities;
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return this.password;
    }

    // Các hàm này trả về true và không liên quan tới DB
    @Override
    @Transient // Báo cho Hibernate: "Đừng có tìm cột này trong DB"
    @JsonIgnore
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    @Transient
    @JsonIgnore
    public boolean isEnabled() {
        return true;
    }
}
