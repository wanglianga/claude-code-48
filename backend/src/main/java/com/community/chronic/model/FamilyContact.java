package com.community.chronic.model;

import jakarta.persistence.*;

/** 家属联系人；proxy=true 表示该家属代管（代测、代收提醒）。 */
@Entity
@Table(name = "family_contacts")
public class FamilyContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long id;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    public ChronicRecord record;

    @Column(nullable = false, length = 64)
    public String name;

    @Column(length = 32)
    public String relation;

    @Column(length = 32)
    public String phone;

    /** 关联的家属平台账号（可空） */
    @ManyToOne(fetch = FetchType.EAGER)
    public UserAccount linkedUser;

    /** 是否家属代管 */
    @Column(nullable = false)
    public boolean proxy = false;
}
