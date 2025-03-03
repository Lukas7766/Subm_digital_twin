package com.digitwin.digitaltwin.repository;

import com.digitwin.digitaltwin.model.GcodeFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface GcodeFileRepository extends JpaRepository<GcodeFile, Long> {
    Optional<GcodeFile> findByPath(String path);
}
