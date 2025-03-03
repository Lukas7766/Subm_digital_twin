package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.config.MaterialConfig;
import com.digitwin.digitaltwin.model.Job;
import com.digitwin.digitaltwin.model.PrinterStatus;
import com.digitwin.digitaltwin.model.RatingRequest;
import com.digitwin.digitaltwin.repository.JobRepository;
import com.digitwin.digitaltwin.repository.PrinterStatusRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestBody;

import java.io.File;
import java.util.List;
import java.util.Optional;

@Service
public class DashboardService {

    private static final Logger logger = LogManager.getLogger(DashboardService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PrinterStatusRepository printerStatusRepository;

    @Autowired
    private OctoPrintService octoPrintService;

    @Autowired
    private MaterialConfig materialConfig;

    @Autowired
    private ErrorDetectionService errorDetectionService;

    @Autowired
    private final TimelapseService timelapseService;

    @Autowired
    private final ZipService zipService;

    public DashboardService(TimelapseService timelapseService,
                            ZipService zipService,
                            ErrorDetectionService errorDetectionService) {
        this.timelapseService = timelapseService;
        this.zipService = zipService;
        this.errorDetectionService = errorDetectionService;
    }

    public JSONObject getCurrentPrinterStatus() {
        Optional<PrinterStatus> latestStatus = printerStatusRepository.findTopByOrderByTimestampDesc();

        if (latestStatus.isPresent()) {
            PrinterStatus status = latestStatus.get();
            JSONObject printerStatus = new JSONObject();
            printerStatus.put("state", status.getState().getText());
            printerStatus.put("nozzle_temperature", status.getTemperature().getTool0().getActual());
            printerStatus.put("bed_temperature", status.getTemperature().getBed().getActual());
            printerStatus.put("operational", status.getState().getFlags().isOperational());
            printerStatus.put("printing", status.getState().getFlags().isPrinting());
            printerStatus.put("paused", status.getState().getFlags().isPaused());

            return printerStatus;
        } else {
            logger.warn("No printer status found in the database.");
            return new JSONObject().put("error", "No printer status available.");
        }
    }

    public JSONArray getAllJobs() {
        List<Job> jobs = jobRepository.findAll();
        JSONArray jobArray = new JSONArray();

        for (Job job : jobs) {
            JSONObject jobJson = new JSONObject();
            jobJson.put("id", job.getId());
            jobJson.put("file_name", job.getFileName());
            jobJson.put("status", job.getStatus().toString());
            jobJson.put("start_time", job.getStartTime());
            jobJson.put("end_time", job.getEndTime());

            jobJson.put("progress", job.getProgress() != null ? job.getProgress() : 0);

            if (job.getGcodeFile() != null) {
                Double medianPrintTime = job.getGcodeFile().getMedianPrintTime();
                Double estimatedPrintTime = job.getGcodeFile().getEstimatedPrintTime();
                jobJson.put("estimated_completion_time", medianPrintTime != null ? medianPrintTime : estimatedPrintTime);
            } else {
                jobJson.put("estimated_completion_time", JSONObject.NULL);
            }

            jobJson.put("filament_used", job.getFilamentUsed());

            jobArray.put(jobJson);
        }

        return jobArray;
    }

    public ResponseEntity<String> rateJob(RatingRequest ratingRequest) {
        Optional<Job> jobOptional = jobRepository.findById(ratingRequest.getJobId());

        if (jobOptional.isPresent()) {
            Job job = jobOptional.get();
            job.setPrintQuality(ratingRequest.getPrintQuality());
            job.setPrintSpeed(ratingRequest.getPrintSpeed());
            job.setFeedback(ratingRequest.getFeedback());

            jobRepository.save(job);
            return ResponseEntity.ok("Rating saved successfully.");
        } else {
            return ResponseEntity.badRequest().body("Job not found.");
        }
    }

    public ResponseEntity<String> pausePrint() {
        return octoPrintService.pausePrint();
    }

    public ResponseEntity<String> continuePrint() {
        if (errorDetectionService.isPausedDueToTemperature()) {
            logger.info("Resuming print after temperature stabilization...");
            errorDetectionService.resumePrintAndRestorePosition();
            return ResponseEntity.ok("Print resumed after temperature stabilization.");
        } else {
            return octoPrintService.resumePrint();
        }
    }

    public ResponseEntity<String> cancelPrint() {
        return octoPrintService.cancelPrint();
    }

    public ResponseEntity<String> preheatNozzle() {
        String material = "PLA";
        double targetTemp = (materialConfig.getMaterialProfiles().get(material).getMaxTemp() +
                materialConfig.getMaterialProfiles().get(material).getMinTemp()) / 2;

        return octoPrintService.preheatNozzle(targetTemp);
    }

    public ResponseEntity<?> getTimelapse(Long jobId) {
        Optional<File> timelapseFile = timelapseService.getTimelapseForJob(jobId);

        if (timelapseFile.isPresent()) {
            File file = timelapseFile.get();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            return ResponseEntity.ok().headers(headers).body(file);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Timelapse not found for Job ID: " + jobId);
        }
    }

    public ResponseEntity<?> getPictureHistory(Long jobId) {
        Optional<File> zipFile = zipService.createZipFromJobImages(jobId);

        if (zipFile.isPresent()) {
            File file = zipFile.get();
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + file.getName());
            return ResponseEntity.ok().headers(headers).body(file);
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No picture history found for Job ID: " + jobId);
        }
    }

    /**
     * Returns the last valid G-code command.
     */
    public ResponseEntity<?> getLastValidGcode() {
        String lastCommand = errorDetectionService.getLastLegalGcodeMovement();
        return ResponseEntity.ok().body("{ \"command\": \"" + lastCommand + "\" }");
    }

    /**
     * Sets the next valid G-code command if the printer is paused due to position.
     */
    public ResponseEntity<?> setNextValidGcode(String command) {
        if (errorDetectionService.isPausedDueToPosition()) {
            errorDetectionService.setNextLegalGcodeMovement(command);
            return ResponseEntity.ok().body("Next valid G-code updated successfully.");
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Printer is not paused due to position.");
        }
    }

    public void draw(){
        errorDetectionService.testSendGcodeInBatches();
    }


}
