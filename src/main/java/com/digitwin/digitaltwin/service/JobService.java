package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.model.GcodeFile;
import com.digitwin.digitaltwin.model.Job;
import com.digitwin.digitaltwin.model.JobStatus;
import com.digitwin.digitaltwin.model.TemperatureLog;
import com.digitwin.digitaltwin.repository.GcodeFileRepository;
import com.digitwin.digitaltwin.repository.JobRepository;
import com.digitwin.digitaltwin.repository.TemperatureLogRepository;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class JobService {

    private static final Logger logger = LogManager.getLogger(JobService.class);

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private PhotoCaptureService photoCaptureService;

    @Autowired
    private TemperatureLogRepository temperatureLogRepository;

    @Autowired
    private ErrorDetectionService errorDetectionService;

    @Autowired
    GcodeFileRepository gcodeFileRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${octoprint.api.url}")
    private String octoprintApiUrl;

    @Value("${octoprint.api.key}")
    private String octoprintApiKey;

    private boolean isLoggingTemperature = false;
    private String currentPrintId;

    @Value("${gcode.storage.path}")
    private String gcodeStoragePath;

    private static final String LOCAL_GCODE_DIR = "gcode_files";

    private static final String LOCAL_PARA_DIR = "parameter_files";
    @Autowired
    private NotificationService notificationService;

    @Transactional
    public Job startJob(String fileName) {
        try {
            logger.info("Attempting to start a new print job...");

            GcodeFile gcodeFile = fetchCurrentlyPrintingGcodeMetadata();
            if (gcodeFile == null) {
                logger.error("Failed to fetch G-code metadata. Aborting job creation.");
                return null;
            }

            Optional<GcodeFile> existingFile = gcodeFileRepository.findByPath(gcodeFile.getPath());

            if (existingFile.isPresent()) {
                gcodeFile = existingFile.get();
                logger.info("Using existing G-code file from database: {}", gcodeFile.getName());
            } else {
                gcodeFile = gcodeFileRepository.save(gcodeFile);
                logger.info("Saved new G-code file: {}", gcodeFile.getName());
            }


            Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();
            if (latestJob.isPresent()) {
                Job lastJob = latestJob.get();
                if (lastJob.getStatus().isActive()) {
                    lastJob.setStatus(JobStatus.FAILED);
                    lastJob.setEndTime(LocalDateTime.now());
                    jobRepository.save(lastJob);
                    logger.warn("Previous job '{}' marked as FAILED due to new print start.", lastJob.getFileName());
                }
            }

            double estimatedPrintTime = (gcodeFile.getMedianPrintTime() != null)
                    ? gcodeFile.getMedianPrintTime()
                    : gcodeFile.getEstimatedPrintTime();
            LocalDateTime estimatedCompletionTime = LocalDateTime.now().plusSeconds((long) estimatedPrintTime);



            Job newJob = new Job(fileName, JobStatus.STARTED, LocalDateTime.now());
            newJob.setGcodeFile(gcodeFile);
            newJob.setEstimatedCompletionTime(estimatedCompletionTime);

            newJob = jobRepository.save(newJob);
            logger.info("New print job started with ID: {}", newJob.getId());

            errorDetectionService.enableErrorDetection();

            if (!existingFile.isPresent()) {
                downloadGcodeFile(gcodeFile, newJob.getId());
            }

            photoCaptureService.startCapturing(newJob.getId().toString());

            startTemperatureLogging(newJob.getId().toString());

            return newJob;

        } catch (Exception e) {
            logger.error("Error starting job: {}", e.getMessage());
            return null;
        }
    }


    public GcodeFile fetchCurrentlyPrintingGcodeMetadata() {
        try {
            String url = octoprintApiUrl + "/files";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Failed to fetch G-code files.");
                return null;
            }

            JSONObject responseJson = new JSONObject(response.getBody());
            JSONArray files = responseJson.getJSONArray("files");

            if (files.length() == 0) {
                logger.warn("No G-code files found.");
                return null;
            }

            JSONObject fileJson = files.getJSONObject(0);

            JSONObject gcodeAnalysis = fileJson.getJSONObject("gcodeAnalysis");

            return new GcodeFile(
                    fileJson.getString("name"),
                    fileJson.getString("path"),
                    fileJson.getString("hash"),
                    fileJson.getString("origin"),
                    fileJson.getLong("size"),
                    LocalDateTime.now(),
                    gcodeAnalysis.getDouble("estimatedPrintTime"),
                    gcodeAnalysis.getJSONObject("filament").getJSONObject("tool0").getDouble("length"),

                    // Printing Area
                    gcodeAnalysis.getJSONObject("dimensions").getDouble("width"),
                    gcodeAnalysis.getJSONObject("dimensions").getDouble("depth"),
                    gcodeAnalysis.getJSONObject("dimensions").getDouble("height"),

                    gcodeAnalysis.getJSONObject("printingArea").getDouble("maxX"),
                    gcodeAnalysis.getJSONObject("printingArea").getDouble("maxY"),
                    gcodeAnalysis.getJSONObject("printingArea").getDouble("maxZ"),
                    gcodeAnalysis.getJSONObject("printingArea").getDouble("minX"),
                    gcodeAnalysis.getJSONObject("printingArea").getDouble("minY"),
                    gcodeAnalysis.getJSONObject("printingArea").getDouble("minZ"),

                    // Travel Area
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("maxX"),
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("maxY"),
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("maxZ"),
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("minX"),
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("minY"),
                    gcodeAnalysis.getJSONObject("travelArea").getDouble("minZ"),

                    gcodeAnalysis.getJSONObject("travelDimensions").getDouble("depth"),
                    gcodeAnalysis.getJSONObject("travelDimensions").getDouble("height"),
                    gcodeAnalysis.getJSONObject("travelDimensions").getDouble("width"),

                    fileJson.getJSONObject("refs").getString("download")
            );

        } catch (Exception e) {
            logger.error("Error fetching G-code metadata: {}", e.getMessage());
            return null;
        }
    }

    public void downloadGcodeFile(GcodeFile gcodeFile, Long jobId) {
        if (gcodeFile == null || jobId == null) {
            logger.error("Invalid G-code file or Job ID is null.");
            return;
        }

        try {
            String jobFolderPath = "src/main/java/com/digitwin/digitaltwin/gcode_files/" + jobId;
            String originalFileName = gcodeFile.getName();
            String filePath = jobFolderPath + "/" + originalFileName;

            File jobFolder = new File(jobFolderPath);
            if (!jobFolder.exists()) {
                jobFolder.mkdirs();
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<byte[]> response = restTemplate.exchange(
                    gcodeFile.getDownloadUrl(),
                    HttpMethod.GET,
                    entity,
                    byte[].class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                Files.write(Paths.get(filePath), response.getBody());
                logger.info("G-code file saved successfully at: {}", filePath);
            } else {
                logger.error("Failed to download G-code file. Status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Error downloading G-code file: {}", e.getMessage());
        }
    }

    @Transactional
    public Job updateJobStatus(JobStatus status, JSONObject payload) {
        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();

        if (latestJob.isPresent()) {
            Job job = latestJob.get();
            job.setStatus(status);

            if (status == JobStatus.FINISHED || status == JobStatus.FAILED || status == JobStatus.CANCELLED) {
                job.setEndTime(LocalDateTime.now());
                job.setStatus(status);
                stopTemperatureLogging();

                errorDetectionService.disableErrorDetection();
                if (status == JobStatus.FINISHED){
                    notificationService.sendWhatsapp("The Print has finished");
                }
                if (status == JobStatus.FAILED){
                    notificationService.sendWhatsapp("The Print has failed");
                }
            }

            logger.info("Updating job status: {} -> {}", job.getFileName(), status);
            jobRepository.save(job);
            logger.info("Job updated successfully with ID: {}", job.getId());

            return job;
        } else {
            logger.warn("No active job found to update with status: {}", status);
            return null;
        }
    }


    private void startTemperatureLogging(String printId) {
        this.isLoggingTemperature = true;
        this.currentPrintId = printId;
        logger.info("Started temperature logging for print ID: {}", printId);
    }

    private void stopTemperatureLogging() {
        this.isLoggingTemperature = false;
        logger.info("Stopped temperature logging for job ID: {}", currentPrintId);
        this.currentPrintId = null;
    }

    @Scheduled(fixedRate = 1000)
    public void logFilamentUsage() {
        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();
        if (latestJob.isPresent() && latestJob.get().getStatus() == JobStatus.STARTED) {
            updateFilamentUsage(latestJob.get().getId());
        }
    }

    private void logTemperature(Long jobId) {
        try {
            Job currentJob = jobRepository.findById(jobId).orElse(null);
            if (currentJob == null) return;

            double tool0Temp = fetchTemperatureFromOctoPrint("tool0");
            double bedTemp = fetchTemperatureFromOctoPrint("bed");

            TemperatureLog tempLog = new TemperatureLog(currentJob, tool0Temp, bedTemp, LocalDateTime.now());
            temperatureLogRepository.save(tempLog);

            logger.info("Logged temperature for Job {}: Nozzle={}°C, Bed={}°C",
                    currentJob.getId(), tool0Temp, bedTemp);
        } catch (Exception e) {
            logger.error("Failed to log temperature: {}", e.getMessage());
        }
    }

    private double fetchTemperatureFromOctoPrint(String type) {
        try {
            String url = octoprintApiUrl + "/printer";

            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());
            return json.getJSONObject("temperature").getJSONObject(type).getDouble("actual");

        } catch (Exception e) {
            logger.error("Error fetching {} temperature from OctoPrint: {}", type, e.getMessage());
            return -1;
        }
    }

    private void updateFilamentUsage(Long jobId) {
        try {
            String url = octoprintApiUrl + "/job";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            JSONObject json = new JSONObject(response.getBody());
            JSONObject filamentJson = json.getJSONObject("job").getJSONObject("filament").getJSONObject("tool0");
            Double filamentUsed = filamentJson.getDouble("length");

            Optional<Job> jobOptional = jobRepository.findById(jobId);
            if (jobOptional.isPresent()) {
                Job job = jobOptional.get();
                job.setFilamentUsed(filamentUsed);
                jobRepository.save(job);
                logger.info("Filament usage updated for Job {}: {} mm", jobId, filamentUsed);
            }
        } catch (Exception e) {
            logger.error("Error fetching filament usage: {}", e.getMessage());
        }
    }

    private void updateJobProgress(Long jobId) {
        try {
            String url = octoprintApiUrl + "/job";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);
            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("Failed to fetch job progress. HTTP Status: {}", response.getStatusCode());
                return;
            }

            JSONObject json = new JSONObject(response.getBody());
            JSONObject progressJSON = json.getJSONObject("progress");

            if (!progressJSON.has("completion") || progressJSON.isNull("completion")) {
                logger.warn("No progress data available from OctoPrint.");
                return;
            }

            Double progress = progressJSON.getDouble("completion"); // OctoPrint reports in 0-100%
            if (progress < 0.0 || progress > 100.0) {
                logger.warn("Invalid progress data received: {}%", progress);
                return;
            }

            Optional<Job> jobOptional = jobRepository.findById(jobId);
            if (jobOptional.isPresent()) {
                Job job = jobOptional.get();
                job.setProgress(progress);
                jobRepository.save(job);
                logger.info("Updated progress for Job {}: {}%", jobId, progress);
            } else {
                logger.warn("Job with ID {} not found. Cannot update progress.", jobId);
            }
        } catch (Exception e) {
            logger.error("Error fetching job progress: {}", e.getMessage());
        }
    }

    @Transactional
    @Scheduled(fixedRate = 1000)
    public void updateJobMetrics() {
        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();

        if (latestJob.isPresent()) {
            Job job = latestJob.get();

            if (job.getStatus() == JobStatus.STARTED || job.getStatus() == JobStatus.RESUMED) {
                updateFilamentUsage(job.getId());
                updateJobProgress(job.getId());
                logTemperature(job.getId());
                errorDetectionService.checkForErrors();
                logger.info("Updated job metrics for Job ID: {}", job.getId());
            } else {
                logger.info("No active print job to update.");
            }
        } else {
            logger.warn("No print job found for updating metrics.");
        }
    }
}
