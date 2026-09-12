package com.community.chronic.repo;

import com.community.chronic.model.FollowUpPlan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface FollowUpPlanRepo extends JpaRepository<FollowUpPlan, Long> {
    List<FollowUpPlan> findByRecordId(Long recordId);
    Optional<FollowUpPlan> findFirstByRecordIdAndActiveTrue(Long recordId);
    List<FollowUpPlan> findByActiveTrueAndNextDueDateLessThanEqual(LocalDate date);
}
