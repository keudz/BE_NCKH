package com.example.bezma.repository;

import com.example.bezma.entity.user.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
        @Query("SELECT u FROM User u " +
                        "LEFT JOIN FETCH u.tenant t " +
                        "LEFT JOIN FETCH u.role r " +
                        "LEFT JOIN FETCH r.permissions " +
                        "LEFT JOIN FETCH u.permissions " +
                        "WHERE u.username = :username")
        Optional<User> findByUsername(@Param("username") String username);

        @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId AND u.isActive = true")
        Page<User> findAllByTenantId(@Param("tenantId") Long tenantId, Pageable pageable);

        @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId " +
                        "AND (LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
                        "OR u.phone LIKE CONCAT('%', :keyword, '%'))")
        Page<User> searchMembersInTenant(@Param("tenantId") Long tenantId, @Param("keyword") String keyword,
                        Pageable pageable);

        Optional<User> findByVerificationToken(String token);

        // --- CHECK TỒN TẠI (Tối ưu performance) ---

        @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.username = :username")
        boolean existsByUsername(@Param("username") String username);

        @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email")
        boolean existsByEmail(@Param("email") String email);

        @Modifying
        @Query("DELETE FROM User u WHERE u.tenant.id = :tenantId")
        void deleteByTenantId(@Param("tenantId") Long tenantId);

        @Query("SELECT u FROM User u WHERE u.tenant.id = :tenantId")
        Optional<User> findFirstByTenantId(@Param("tenantId") Long tenantId);

        Optional<User> findByZaloId(String zaloId);

        Optional<User> findByPhone(String phone);

        // Lấy toàn bộ danh sách nhân viên của 1 doanh nghiệp theo trạng thái xóa
        List<User> findAllByTenantIdAndIsDeleted(Long tenantId, Boolean isDeleted);

        // Lấy toàn bộ danh sách nhân viên của 1 doanh nghiệp (Không phân trang)
        List<User> findAllByTenantId(Long tenantId);

        // Thêm hàm này nếu chưa có để check trùng lặp khi đổi email
        Optional<User> findByEmail(String email);

        Optional<User> findByIdAndTenantId(Long id, Long tenantId);

}