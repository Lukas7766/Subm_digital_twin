package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.config.MaterialConfig;
import com.digitwin.digitaltwin.model.GcodeFile;
import com.digitwin.digitaltwin.model.Job;
import com.digitwin.digitaltwin.model.JobStatus;
import com.digitwin.digitaltwin.repository.JobRepository;
import jakarta.annotation.PostConstruct;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ErrorDetectionService {

    private static final Logger logger = LogManager.getLogger(ErrorDetectionService.class);

    @Autowired
    private JobRepository jobRepository;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private OctoPrintService octoPrintService;
    @Autowired
    private MaterialConfig materialConfig;

    @Value("${error_detection.enable}")
    private boolean errorDetectionEnabled;

    @Value("${error_detection.max_pause_duration}")
    private int maxPauseDuration;

    @Value("${error_detection.notify.enable_email}")
    private boolean enableEmail;

    @Value("${error_detection.notify.enable_whatsapp}")
    private boolean enableWhatsapp;

    private boolean heatingMode = true;
    private boolean printFailed = false;
    private boolean pausedDueToTemperature = false;
    private boolean pausedDueToPosition = false;
    private boolean pausedMessageLogged = false;
    private double lastZPosition = 0.0;
    private String lastLegalGcodeMovement = "UNKNOWN";
    private String nextLegalGcodeMovement = "UNKNOWN";
    private double currentX = 0.0;
    private double currentY = 0.0;
    private double currentZ = 0.0;
    private boolean canceledByTwin = false;
    @Autowired
    private EventService eventService;

    @PostConstruct
    public void resetErrorDetection() {
        errorDetectionEnabled = false;
        logger.info("Error detection disabled on startup.");
    }

    public void setCanceledByTwin(boolean status) {
        this.canceledByTwin = status;
    }

    public boolean isCanceledByTwin() {
        return this.canceledByTwin;
    }


    public void enableErrorDetection() {
        errorDetectionEnabled = true;
        heatingMode = true;
        printFailed = false;
        pausedDueToTemperature = false;
        pausedMessageLogged = false;
        lastZPosition = 0.0;
        logger.info("Error detection enabled.");
    }

    public void disableErrorDetection() {
        errorDetectionEnabled = false;
        logger.info("Error detection disabled.");
    }

    public void updateCurrentPosition(double x, double y, double z) {
        this.currentX = x;
        this.currentY = y;
        this.currentZ = z;
        logger.info("Updated current position: X={}, Y={}, Z={}", x, y, z);
    }

    public void resumePrintFromGcode(String gcodeLine, String gcodeFilePath) {
        logger.info("Resuming print from G-code file: {}", gcodeFilePath);

        File gcodeFile = Paths.get(gcodeFilePath).toFile();
        if (!gcodeFile.exists()) {
            logger.error("G-code file not found: {}", gcodeFilePath);
            return;
        }

        List<String> gcodeCommands = new ArrayList<>();
        boolean foundStart = false;

        // Read G-code file
        try (BufferedReader reader = new BufferedReader(new FileReader(gcodeFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!foundStart) {
                    if (line.trim().equalsIgnoreCase(gcodeLine.trim())) {
                        foundStart = true;  // Start collecting commands after this line
                    }
                } else {
                    if (!line.startsWith(";")) {  // Ignore comments
                        gcodeCommands.add(line.trim());
                    }
                }
            }
        } catch (IOException e) {
            logger.error("Error reading G-code file: {}", e.getMessage());
            return;
        }

        if (gcodeCommands.isEmpty()) {
            logger.error("No valid G-code commands found after the specified line.");
            return;
        }

        logger.info("Found {} commands to resume printing.", gcodeCommands.size());
        sendGcodeInBatches(gcodeCommands);
    }

    public void testSendGcodeInBatches() {
        List<String> testGcodeCommands = List.of(
                "G28",  // Home all axes
                "G92 E0",  // Reset extruder
                "G1 Z2.0 F3000",  // Move Z up
                "G1 X10.1 Y20 Z0.28 F5000.0",  // Move to start position
                "G1 X10.1 Y200.0 Z0.28 F1500.0 E15",  // Draw first line
                "G1 X10.4 Y200.0 Z0.28 F5000.0",  // Move slightly to the side
                "G1 X10.4 Y20 Z0.28 F1500.0 E30",  // Draw second line
                "G92 E0",  // Reset extruder
                "G1 Z2.0 F3000"  // Move Z up again
        );
        sendGcodeInBatches(testGcodeCommands);
    }

    private void sendGcodeInBatches(List<String> gcodeCommands) {
        int batchSize = 10;

        for (int i = 0; i < gcodeCommands.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, gcodeCommands.size());
            List<String> batch = gcodeCommands.subList(i, endIndex);

            for (String command : batch) {
                logger.info("Sending G-code: {}", command);
                octoPrintService.sendGcodeCommand(command);
            }

            try {
                logger.info("Waiting 2 seconds before sending next batch...");
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                logger.error("Error during wait time: {}", e.getMessage());
                Thread.currentThread().interrupt();
            }
        }

        logger.info("All G-code commands have been sent.");
    }

    public void resetPrintFailed() {
        this.printFailed = false;
    }

    public String getLastLegalGcodeMovement() {
        return lastLegalGcodeMovement;
    }

    public String getNextLegalGcodeMovement() {
        return nextLegalGcodeMovement;
    }

    public boolean isPausedDueToTemperature() {
        return pausedDueToTemperature;
    }

    @Scheduled(fixedRate = 1000)
    public void checkForErrors() {
        if (!errorDetectionEnabled) return;

        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();
        if (latestJob.isPresent()) {
            Job job = latestJob.get();
            JobStatus status = job.getStatus();
            LocalDateTime now = LocalDateTime.now();

            double currentNozzleTemp = octoPrintService.getNozzleTemperature();

            checkPrintheadOutOfBounds(job);
            checkPauseDuration(job, now);
            checkTemperatureErrors(job, currentNozzleTemp);

            if (status == JobStatus.FAILED && !printFailed) {
                triggerAlert("Print failed!", job);
                printFailed = true;
            }
        }
    }

    private void checkPrintheadOutOfBounds(Job job) {
        GcodeFile gcodeFile = job.getGcodeFile();
        if (gcodeFile == null) return;

        Optional<Double[]> maxTravelLimits = octoPrintService.getMaxTravelLimits(gcodeFile);
        if (maxTravelLimits.isEmpty()) return;

        Double[] limits = maxTravelLimits.get();
        double maxX = limits[0], maxY = limits[1], maxZ = limits[2];

        if (octoPrintService.getCurrentX() > maxX || octoPrintService.getCurrentY() > maxY || octoPrintService.getCurrentZ() > maxZ
                || octoPrintService.getCurrentX() < 0 || octoPrintService.getCurrentY() < 0 || octoPrintService.getCurrentZ() < 0) {

            if (!pausedDueToPosition) {
                triggerAlert("Print head out of bounds! X=" + octoPrintService.getCurrentX() +
                        " Y=" + octoPrintService.getCurrentY() +
                        " Z=" + octoPrintService.getCurrentZ(), job);
                pausePrintDueToPosition();
                pausedDueToPosition = true;
                canceledByTwin = true;
                octoPrintService.cancelPrint();
            }
        }
    }

    private void checkPauseDuration(Job job, LocalDateTime now) {
        if (job.getStatus() == JobStatus.PAUSED) {
            Duration pauseDuration = Duration.between(
                    job.getEndTime() != null ? job.getEndTime() : job.getStartTime(), now
            );
            if (pauseDuration.toHours() >= maxPauseDuration) {
                triggerAlert("Print paused for too long (>" + maxPauseDuration + "h)", job);
            }
        }
    }


    private void pausePrintDueToPosition() {
        logger.warn("Pausing print due to out-of-bounds movement.");
        pausedDueToPosition = true;

        ResponseEntity<String> pauseResponse = octoPrintService.pausePrint();
        if (!pauseResponse.getStatusCode().is2xxSuccessful()) {
            logger.error("Failed to pause print: {}", pauseResponse.getBody());
            return;
        }

        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();
        if (latestJob.isEmpty()) {
            logger.error("No active job found to check G-code.");
            return;
        }
        Job job = latestJob.get();

        lastLegalGcodeMovement = octoPrintService.fetchLastLegalGcodeCommand(job.getGcodeFile());
        nextLegalGcodeMovement = octoPrintService.determineNextValidGcode(job.getGcodeFile(), lastLegalGcodeMovement);

        logger.info("Paused print. Last legal movement: {} | Next suggested move: {}", lastLegalGcodeMovement, nextLegalGcodeMovement);
    }


    private String determineNextLegalMovement() {
        if ("UNKNOWN".equals(lastLegalGcodeMovement)) return "UNKNOWN";
        return lastLegalGcodeMovement.replaceAll("X\\d+", "X0").replaceAll("Y\\d+", "Y0").replaceAll("Z\\d+", "Z10");
    }

    private void checkTemperatureErrors(Job job, double nozzleTemp) {
        String material = detectMaterial();
        double materialMinTemp = getMaterialMinTemp(material);
        double materialMaxTemp = getMaterialMaxTemp(material);

        if (heatingMode && nozzleTemp >= materialMinTemp) {
            heatingMode = false;
            logger.info("Printer temperature stabilized at {}°C for {}", nozzleTemp, material);
        }

        if (heatingMode) {
            logger.info("Skipping error check: Printer is heating ({}°C)", nozzleTemp);
            return;
        }

        if (pausedDueToTemperature) {
            if (!pausedMessageLogged) {
                logger.info("Print paused due to temperature. Waiting for user action.");
                pausedMessageLogged = true;
            }
            return;
        }

        if (nozzleTemp > materialMaxTemp || nozzleTemp < materialMinTemp) {
            triggerAlert("Nozzle temperature out of range: " + nozzleTemp + "°C", job);
            pausePrintDueToTemperature();
        }
    }

    private void pausePrintDueToTemperature() {
        if (pausedDueToTemperature) return;

        logger.warn("Pausing print due to critical temperature issue.");
        pausedDueToTemperature = true;

        ResponseEntity<String> pauseResponse = octoPrintService.pausePrint();
        if (!pauseResponse.getStatusCode().is2xxSuccessful()) {
            logger.error("Failed to pause print: {}", pauseResponse.getBody());
            return;
        }

        lastZPosition = currentZ;
        logger.info("Stored last Z position: {}mm", lastZPosition);

        ResponseEntity<String> moveResponse = octoPrintService.sendGcodeCommand("G91");
        if (moveResponse.getStatusCode().is2xxSuccessful()) {
            octoPrintService.sendGcodeCommand("G1 Z20 F300");
            octoPrintService.sendGcodeCommand("G90");
            logger.info("Raised print head by 2 cm.");
        } else {
            logger.error("Failed to move print head: {}", moveResponse.getBody());
        }

        logger.info("Print paused. Waiting for user action.");
    }

    public void resumePrintAndRestorePosition() {
        logger.info("Resuming print after temperature issue...");
        pausedDueToTemperature = false;
        pausedMessageLogged = false;

        if (lastZPosition > 0) {
            String moveCommand = "G1 Z" + lastZPosition + " F300";
            ResponseEntity<String> moveDownResponse = octoPrintService.sendGcodeCommand(moveCommand);

            if (moveDownResponse.getStatusCode().is2xxSuccessful()) {
                logger.info("Restored print head to original position at {}mm.", lastZPosition);
            } else {
                logger.error("Failed to restore print head position: {}", moveDownResponse.getBody());
            }
        } else {
            logger.warn("No stored Z position found, skipping restore.");
        }

        octoPrintService.resumePrint();
    }

    public void resumePrintFromOutOfBounds() {
        logger.info("Resuming print from out-of-bounds state...");
        pausedDueToPosition = false;

        ResponseEntity<String> moveBackResponse = octoPrintService.sendGcodeCommand(nextLegalGcodeMovement);
        if (!moveBackResponse.getStatusCode().is2xxSuccessful()) {
            logger.error("Failed to move back to last legal position: {}", moveBackResponse.getBody());
        } else {
            logger.info("Restored print head to next legal position.");
        }

        octoPrintService.resumePrint();
    }

    private void triggerAlert(String message, Job job) {
        logger.warn("Error detected for job {}: {}", job.getId(), message);
        if (enableEmail) {
            notificationService.sendEmail("Print Error Alert", message);
        }
        if (enableWhatsapp) {
            notificationService.sendWhatsapp("Print Error Alert: " + message);
        }
    }

    public void setNextLegalGcodeMovement(String command) {
        this.nextLegalGcodeMovement = command;
        logger.info("Next valid G-code set to: {}", command);
    }

    public boolean isPausedDueToPosition() {
        return pausedDueToPosition;
    }

    private String detectMaterial() {
        return "PLA";
    }

    private double getMaterialMaxTemp(String material) {
        return materialConfig.getMaterialProfiles().get(material).getMaxTemp();
    }

    private double getMaterialMinTemp(String material) {
        return materialConfig.getMaterialProfiles().get(material).getMinTemp();
    }
}
