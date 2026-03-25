package com.example.bezma.entity.auth;

import jakarta.persistence.*;
import lombok.*;
import com.example.bezma.common.base.BaseEntity;
import com.example.bezma.entity.user.User;

import java.time.Instant;

@Entity
@Getter @Setter
@Builder
@NoArgsConstructor @AllArgsConstructor
public class RefreshToken extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id")
    private User user;
    @Column(nullable = false)
    private Instant expiryDate;
}
