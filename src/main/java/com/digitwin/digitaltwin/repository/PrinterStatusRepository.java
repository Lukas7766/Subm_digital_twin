package com.digitwin.digitaltwin.repository;

import com.digitwin.digitaltwin.model.PrinterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PrinterStatusRepository extends JpaRepository<PrinterStatus, Long> {
    Optional<PrinterStatus> findTopByOrderByTimestampDesc();
}
