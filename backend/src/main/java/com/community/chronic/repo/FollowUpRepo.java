package com.community.chronic.repo;

import com.community.chronic.model.FollowUp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FollowUpRepo extends JpaRepository<FollowUp, Long> {
    List<FollowUp> findByRecordIdOrderByVisitDateDesc(Long recordId);
}
