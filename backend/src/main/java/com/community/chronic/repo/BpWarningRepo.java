package com.community.chronic.repo;

import com.community.chronic.model.BpWarning;
import com.community.chronic.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface BpWarningRepo extends JpaRepository<BpWarning, Long> {
    List<BpWarning> findByRecordIdOrderByCreatedAtDesc(Long recordId);
    List<BpWarning> findByStatusOrderByCreatedAtDesc(Enums.WarningStatus status);
    List<BpWarning> findByRecordDoctorIdAndStatusOrderByCreatedAtDesc(Long doctorId, Enums.WarningStatus status);
    boolean existsByRecordIdAndStatusIn(Long recordId, Collection<Enums.WarningStatus> statuses);
    Optional<BpWarning> findFirstByRecordIdAndStatusInOrderByCreatedAtDesc(Long recordId, Collection<Enums.WarningStatus> statuses);
    long countByStatus(Enums.WarningStatus status);
}
