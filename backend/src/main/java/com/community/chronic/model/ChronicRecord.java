package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 慢病档案：一个居民一种慢病一份档案，所有事件回流到这里。 */
@Entity
@Table(name = "chronic_records")
public class ChronicRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount resident;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount doctor;

    @ManyToOne(fetch = FetchType.EAGER)
    public UserAccount nurse;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.DiseaseType diseaseType;

    @Column(length = 512)
    public String diagnosis;

    /** 并发症 */
    @Column(length = 512)
    public String complications;

    /** 目标血压 */
    public Integer targetSys = 140;
    public Integer targetDia = 90;

    /** 目标血糖范围（mmol/L） */
    public Double glucoseMin = 3.9;
    public Double glucoseMax = 7.8;

    /** 医保签约类型 */
    @Column(length = 64)
    public String insurance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.FamilySupport familySupport = Enums.FamilySupport.MODERATE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.ManageLevel manageLevel = Enums.ManageLevel.NORMAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.RecordStatus status = Enums.RecordStatus.ACTIVE;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    public LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void touch() {
        this.updatedAt = LocalDateTime.now();
    }
}
