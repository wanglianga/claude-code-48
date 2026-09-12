package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/** 随访计划：纳入统一慢病计划（家庭监测 + 用药提醒 + 线下随访）。 */
@Entity
@Table(name = "follow_up_plans")
public class FollowUpPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.PlanType planType = Enums.PlanType.PHONE;

    /** 随访间隔天数 */
    public Integer intervalDays = 30;

    public LocalDate nextDueDate;

    @Column(nullable = false)
    public boolean active = true;
}
