package com.community.chronic.repo;

import com.community.chronic.model.Hospitalization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HospitalizationRepo extends JpaRepository<Hospitalization, Long> {
    List<Hospitalization> findByRecordIdOrderByStartDateDesc(Long recordId);
}
