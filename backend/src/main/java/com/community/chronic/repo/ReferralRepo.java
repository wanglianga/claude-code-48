package com.community.chronic.repo;

import com.community.chronic.model.Referral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReferralRepo extends JpaRepository<Referral, Long> {
    List<Referral> findByRecordIdOrderByCreatedAtDesc(Long recordId);
}
