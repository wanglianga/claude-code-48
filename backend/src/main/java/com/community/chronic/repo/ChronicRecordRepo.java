package com.community.chronic.repo;

import com.community.chronic.model.ChronicRecord;
import com.community.chronic.model.Enums;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChronicRecordRepo extends JpaRepository<ChronicRecord, Long> {
    List<ChronicRecord> findByDoctorId(Long doctorId);
    List<ChronicRecord> findByResidentId(Long residentId);
    Optional<ChronicRecord> findFirstByResidentId(Long residentId);
    List<ChronicRecord> findByStatus(Enums.RecordStatus status);
    long countByStatus(Enums.RecordStatus status);
    long countByManageLevel(Enums.ManageLevel level);
}
