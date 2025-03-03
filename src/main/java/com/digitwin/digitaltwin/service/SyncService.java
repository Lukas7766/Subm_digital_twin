package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.model.PrinterStatus;
import com.digitwin.digitaltwin.repository.PrinterStatusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SyncService {

    private final PrinterStatusRepository printerStatusRepository;

    @Autowired
    private OctoPrintService octoPrintService;

    public SyncService(PrinterStatusRepository printerStatusRepository) {
        this.printerStatusRepository = printerStatusRepository;
    }

    @Scheduled(fixedRate = 1000) // Sync every 5 minutes
    public void syncPrinterStatus() {
        PrinterStatus printerStatus = octoPrintService.getPrinterStatus();
        printerStatusRepository.save(printerStatus);
    }
}
