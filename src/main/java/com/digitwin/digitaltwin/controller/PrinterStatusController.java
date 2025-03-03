package com.digitwin.digitaltwin.controller;


import com.digitwin.digitaltwin.model.PrinterStatus;
import com.digitwin.digitaltwin.repository.PrinterStatusRepository;
import com.digitwin.digitaltwin.service.OctoPrintService;
import com.digitwin.digitaltwin.service.SyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.print.PrintService;
import java.util.Optional;

@RestController
@RequestMapping("/api")
public class PrinterStatusController {

    @Autowired
    private OctoPrintService octoPrintService;
    @Autowired
    private PrinterStatusRepository printerStatusRepository;

    @GetMapping("/status")
    public ResponseEntity<PrinterStatus> getPrinterStatus() {
        Optional<PrinterStatus> printerStatusOpt = printerStatusRepository.findTopByOrderByTimestampDesc();

        if (printerStatusOpt.isEmpty()) {
            return ResponseEntity.notFound().build(); // Return 404 if no status found
        }

        return ResponseEntity.ok(printerStatusOpt.get()); // Return the latest status directly
    }
}