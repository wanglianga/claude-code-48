package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDate;

/** 用药方案条目；停药/换药不删除，置为 STOPPED 并写事件，保留变更历史。 */
@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Column(nullable = false, length = 128)
    public String name;

    /** 剂量，如 5mg、1片 */
    @Column(length = 64)
    public String dosage;

    /** 每日次数 */
    public Integer timesPerDay = 1;

    /** 服药时间点，逗号分隔，如 08:00,18:00 */
    @Column(length = 64)
    public String timeSlots = "08:00";

    public LocalDate startDate = LocalDate.now();

    public LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.MedStatus status = Enums.MedStatus.ACTIVE;

    @Column(length = 256)
    public String note;
}
