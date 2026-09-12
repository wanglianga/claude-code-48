package com.community.chronic.model;

import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 住院记录。 */
@Entity
@Table(name = "hospitalizations")
public class Hospitalization {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Column(nullable = false, length = 128)
    public String hospital;

    @Column(length = 512)
    public String reason;

    public LocalDate startDate;

    public LocalDate endDate;

    @Column(length = 512)
    public String note;

    @Column(nullable = false)
    public LocalDateTime createdAt = LocalDateTime.now();
}
