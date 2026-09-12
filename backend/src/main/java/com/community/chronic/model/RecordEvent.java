package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 档案事件流：药物变更、家属代管、住院、失访、转诊结果等全部回流到同一档案。 */
@Entity
@Table(name = "record_events", indexes = {
        @Index(columnList = "record_id, createdAt")
})
public class RecordEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Enums.EventType eventType;

    @Column(nullable = false, length = 1024)
    public String detail;

    /** 操作人姓名 */
    @Column(length = 64)
    public String createdBy;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
