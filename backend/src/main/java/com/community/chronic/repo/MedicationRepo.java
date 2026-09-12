package com.community.chronic.repo;

import com.community.chronic.model.Enums;
import com.community.chronic.model.Medication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MedicationRepo extends JpaRepository<Medication, Long> {
    List<Medication> findByRecordId(Long recordId);
    List<Medication> findByRecordIdAndStatus(Long recordId, Enums.MedStatus status);
}
