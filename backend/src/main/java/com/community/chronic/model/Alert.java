package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/** 异常告警：推送给社区护士、医生和家属的任务。 */
@Entity
@Table(name = "alerts", indexes = {
        @Index(columnList = "record_id, status")
})
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    public Enums.AlertType alertType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.AlertLevel level = Enums.AlertLevel.WARN;

    @Column(nullable = false, length = 512)
    public String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    public Enums.AlertStatus status = Enums.AlertStatus.OPEN;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();

    @ManyToOne(fetch = FetchType.EAGER)
    public UserAccount handledBy;

    public LocalDateTime handledAt;

    @Column(length = 512)
    public String handleNote;
}
