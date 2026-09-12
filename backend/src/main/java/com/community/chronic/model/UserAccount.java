package com.community.chronic.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
public class UserAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @Column(nullable = false, unique = true, length = 64)
    public String username;

    @JsonIgnore
    @Column(nullable = false)
    public String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.Role role;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(length = 32)
    public String phone;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public UserAccount() {}

    public UserAccount(String username, String passwordHash, Enums.Role role, String name, String phone) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
        this.name = name;
        this.phone = phone;
    }
}
