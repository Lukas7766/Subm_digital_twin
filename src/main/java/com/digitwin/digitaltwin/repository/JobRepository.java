package com.digitwin.digitaltwin.repository;

import com.digitwin.digitaltwin.model.Job;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface JobRepository extends JpaRepository<Job, Long> {
    Optional<Job> findTopByOrderByStartTimeDesc();

    @Modifying
    @Transactional
    @Query("UPDATE Job j SET j.filamentUsed = :filament WHERE j.id = :jobId")
    void updateFilamentUsage(Long jobId, Double filament);
}
