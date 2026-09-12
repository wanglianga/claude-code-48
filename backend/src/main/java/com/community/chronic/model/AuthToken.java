package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 登录令牌（演示用，存数据库）。 */
@Entity
@Table(name = "auth_tokens")
public class AuthToken {

    @Id
    @Column(length = 64)
    public String token;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount user;

    @Column(nullable = false)
    public LocalDateTime expiresAt;

    public AuthToken() {}

    public AuthToken(String token, UserAccount user, LocalDateTime expiresAt) {
        this.token = token;
        this.user = user;
        this.expiresAt = expiresAt;
    }
}
