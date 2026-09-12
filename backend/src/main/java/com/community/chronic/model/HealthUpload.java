package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 家庭上传记录（单表多类型）：
 * BP=血压，GLUCOSE=血糖，MEDICATION=用药执行，DIET=饮食，EXERCISE=运动。
 */
@Entity
@Table(name = "health_uploads", indexes = {
        @Index(columnList = "record_id, type, measuredAt")
})
public class HealthUpload {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public UserAccount uploader;

    /** 居民自测 / 家属代测 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.UploaderType uploaderType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.UploadType type;

    // ---- BP ----
    public Integer sys;
    public Integer dia;
    public Integer heartRate;

    // ---- GLUCOSE ----
    public Double glucose;

    // ---- MEDICATION ----
    @Column(length = 128)
    public String medicationName;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    public Enums.MedLogStatus medStatus;

    // ---- DIET / EXERCISE ----
    @Column(length = 512)
    public String dietNote;

    @Column(length = 512)
    public String exerciseNote;

    /** 备注（不良反应描述等） */
    @Column(length = 512)
    public String note;

    /** 实际测量/发生时间 */
    @Column(nullable = false)
    public LocalDateTime measuredAt = LocalDateTime.now();

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
