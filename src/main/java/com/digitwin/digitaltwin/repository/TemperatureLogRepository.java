package com.digitwin.digitaltwin.repository;

import com.digitwin.digitaltwin.model.TemperatureLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TemperatureLogRepository extends JpaRepository<TemperatureLog, Long> {
    List<TemperatureLog> findByJobId(Long jobId);
}
