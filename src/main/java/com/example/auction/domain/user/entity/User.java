package com.example.auction.domain.user.entity;

import com.example.auction.common.entity.DeletableEntity;
import com.example.auction.domain.user.consts.UserRole;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends DeletableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    private BigDecimal rating;

    public static User of(String email, String encodedPassword, UserRole role) {
        User user = new User();
        user.email = email;
        user.password = encodedPassword;
        user.role = role;
        user.rating = null;

        return user;
    }
}
