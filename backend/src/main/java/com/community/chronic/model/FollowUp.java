package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 随访记录：医生查看趋势/依从性/家属反馈/近期就医后的处置结论。 */
@Entity
@Table(name = "follow_ups")
public class FollowUp {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount doctor;

    @Column(nullable = false)
    public LocalDate visitDate = LocalDate.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.PlanType mode = Enums.PlanType.PHONE;

    /** 随访摘要（症状、血压控制情况等） */
    @Column(length = 1024)
    public String summary;

    /** 用药依从性评估 */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    public Enums.Adherence adherence;

    /** 家属反馈 */
    @Column(length = 512)
    public String familyFeedback;

    /** 近期就医情况 */
    @Column(length = 512)
    public String recentMedical;

    /** 处置：调整提醒 / 建议复诊 / 转诊上级医院 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Enums.FollowUpDecision decision = Enums.FollowUpDecision.NONE;

    @Column(length = 512)
    public String decisionDetail;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
