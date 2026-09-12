package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 转诊上级医院；结果回填后写事件回档案。 */
@Entity
@Table(name = "referrals")
public class Referral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount doctor;

    @Column(nullable = false, length = 128)
    public String toHospital;

    @Column(length = 512)
    public String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.ReferralStatus status = Enums.ReferralStatus.OPEN;

    /** 转诊结果（上级医院反馈） */
    @Column(length = 1024)
    public String result;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    public LocalDateTime completedAt;
}
