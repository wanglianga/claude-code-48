package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 连续高血压预警：居民连续多次上传超标血压（或出现危急值）时生成。
 * 流程：居民/家属补充测量信息 → 护士电话确认（记录症状和用药）→ 医生处置。
 * 预警生成时自动生成电话随访任务。
 */
@Entity
@Table(name = "bp_warnings", indexes = {
        @Index(columnList = "record_id, status")
})
public class BpWarning {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Enums.WarningStatus status = Enums.WarningStatus.OPEN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.AlertLevel level = Enums.AlertLevel.WARN;

    /** 连续超标次数（危急值触发时为 1） */
    public Integer triggerCount = 1;

    /** 触发时最高血压 */
    public Integer maxSys;
    public Integer maxDia;

    @Column(nullable = false, length = 512)
    public String message;

    // ---- 居民/家属补充信息 ----
    /** 测量时间说明，如「近三天早晨 7 点」 */
    @Column(length = 128)
    public String suppMeasuredAt;

    /** 测量前是否已服药 */
    public Boolean tookMed;

    /** 症状 */
    @Column(length = 512)
    public String symptoms;

    /** 是否已就医 */
    public Boolean sawDoctor;

    @Column(length = 64)
    public String suppBy;

    public LocalDateTime suppAt;

    // ---- 护士电话确认（记录症状和用药情况） ----
    @ManyToOne(fetch = FetchType.EAGER)
    public UserAccount nurse;

    public LocalDateTime nurseConfirmedAt;

    @Column(length = 512)
    public String nurseSymptoms;

    @Column(length = 512)
    public String nurseMedNote;

    @Column(length = 512)
    public String nurseNote;

    // ---- 医生处置 ----
    @ManyToOne(fetch = FetchType.EAGER)
    public UserAccount doctor;

    @Enumerated(EnumType.STRING)
    @Column(length = 24)
    public Enums.DoctorAction doctorAction;

    @Column(length = 512)
    public String doctorNote;

    public LocalDateTime doctorHandledAt;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
