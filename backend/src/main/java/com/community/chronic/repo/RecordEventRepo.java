package com.community.chronic.repo;

import com.community.chronic.model.RecordEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordEventRepo extends JpaRepository<RecordEvent, Long> {
    List<RecordEvent> findByRecordIdOrderByCreatedAtDesc(Long recordId);
}
