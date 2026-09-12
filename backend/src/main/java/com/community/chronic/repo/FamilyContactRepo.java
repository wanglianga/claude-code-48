package com.community.chronic.repo;

import com.community.chronic.model.FamilyContact;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FamilyContactRepo extends JpaRepository<FamilyContact, Long> {
    List<FamilyContact> findByRecordId(Long recordId);
    List<FamilyContact> findByLinkedUserId(Long userId);
}
