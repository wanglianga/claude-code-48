package com.community.chronic.repo;

import com.community.chronic.model.Alert;
import com.community.chronic.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AlertRepo extends JpaRepository<Alert, Long> {
    List<Alert> findByRecordIdOrderByCreatedAtDesc(Long recordId);
    List<Alert> findByStatusOrderByCreatedAtDesc(Enums.AlertStatus status);
    List<Alert> findByRecordDoctorIdAndStatusOrderByCreatedAtDesc(Long doctorId, Enums.AlertStatus status);
    boolean existsByRecordIdAndAlertTypeAndStatus(Long recordId, Enums.AlertType type, Enums.AlertStatus status);
    boolean existsByRecordIdAndAlertTypeAndLevelAndStatus(Long recordId, Enums.AlertType type, Enums.AlertLevel level, Enums.AlertStatus status);
    long countByStatus(Enums.AlertStatus status);
    long countByRecordIdAndCreatedAtAfter(Long recordId, java.time.LocalDateTime after);
    long countByRecordIdAndAlertTypeAndCreatedAtAfter(Long recordId, Enums.AlertType type, java.time.LocalDateTime after);
}
