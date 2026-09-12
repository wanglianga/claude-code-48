package com.community.chronic.repo;

import com.community.chronic.model.Enums;
import com.community.chronic.model.HealthUpload;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface HealthUploadRepo extends JpaRepository<HealthUpload, Long> {
    List<HealthUpload> findByRecordIdAndTypeAndMeasuredAtAfterOrderByMeasuredAtAsc(
            Long recordId, Enums.UploadType type, LocalDateTime after);

    List<HealthUpload> findByRecordIdAndMeasuredAtAfterOrderByMeasuredAtDesc(Long recordId, LocalDateTime after);

    List<HealthUpload> findByRecordIdOrderByMeasuredAtDesc(Long recordId);

    Optional<HealthUpload> findFirstByRecordIdOrderByMeasuredAtDesc(Long recordId);

    List<HealthUpload> findTop3ByRecordIdAndTypeOrderByMeasuredAtDesc(Long recordId, Enums.UploadType type);

    long countByRecordIdAndTypeAndMedStatusAndMeasuredAtAfter(
            Long recordId, Enums.UploadType type, Enums.MedLogStatus medStatus, LocalDateTime after);

    long countByRecordIdAndTypeAndMeasuredAtAfter(Long recordId, Enums.UploadType type, LocalDateTime after);

    List<HealthUpload> findByUploaderIdOrderByCreatedAtDesc(Long uploaderId);
}
