package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.model.Job;
import com.digitwin.digitaltwin.model.JobStatus;
import com.digitwin.digitaltwin.repository.JobRepository;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class EventService {

    private static final Logger eventLogger = LogManager.getLogger("EventLogger");

    @Autowired
    private PhotoCaptureService photoCaptureService;

    @Autowired
    private JobService jobService;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private TimelapseService timelapseService;

    @Autowired
    private ErrorDetectionService errorDetectionService;

    @Value("${logging.pause_photo_on_print_pause}")
    private boolean pausePhotoOnPrintPause;

    public void processEvent(String eventPayload) {
        JSONObject json = new JSONObject(eventPayload);

        String event = json.getString("event");
        System.out.println("Received event: " + event);

        eventLogger.info("Received Event: {}", event);

        switch (event) {
            case "PrintStarted":
                handlePrintStarted(json.getJSONObject("payload"));
                break;
            case "PrintDone":
                handlePrintDone(json.getJSONObject("payload"));
                break;
            case "PrintFailed":
                handlePrintFailed(json.getJSONObject("payload"));
                break;
            case "PrintPaused":
                handlePrintPaused(json.getJSONObject("payload"));
                break;
            case "PrintResumed":
                handlePrintResumed(json.getJSONObject("payload"));
                break;
            case "PrintCancelled":
                handlePrintCancelled(json.getJSONObject("payload"));
                break;
            case "Startup":
                handleStartup();
                break;
            case "Shutdown":
                handleShutdown();
                break;
            case "ClientOpened":
                handleClientOpened(json.getJSONObject("payload"));
                break;
            case "ClientClosed":
                handleClientClosed(json.getJSONObject("payload"));
                break;
            case "ClientAuthed":
                handleClientAuthed(json.getJSONObject("payload"));
                break;
            case "UserLoggedIn":
                handleUserLoggedIn(json.getJSONObject("payload"));
                break;
            case "UserLoggedOut":
                handleUserLoggedOut(json.getJSONObject("payload"));
                break;
            case "FileAdded":
                handleFileAdded(json.getJSONObject("payload"));
                break;
            case "FileMoved":
                handleFileMoved(json.getJSONObject("payload"));
                break;
            case "FileRemoved":
                handleFileRemoved(json.getJSONObject("payload"));
                break;
            case "FolderAdded":
                handleFolderAdded(json.getJSONObject("payload"));
                break;
            case "FolderRemoved":
                handleFolderRemoved(json.getJSONObject("payload"));
                break;
            case "CaptureStart":
                handleCaptureStart(json.getJSONObject("payload"));
                break;
            case "CaptureDone":
                handleCaptureDone(json.getJSONObject("payload"));
                break;
            case "CaptureFailed":
                handleCaptureFailed(json.getJSONObject("payload"));
                break;
            case "MovieRendering":
                handleMovieRendering(json.getJSONObject("payload"));
                break;
            case "MovieDone":
                handleMovieDone(json.getJSONObject("payload"));
                break;
            case "MovieFailed":
                handleMovieFailed(json.getJSONObject("payload"));
                break;
            case "PrinterConnected":
                handlePrinterConnected(json.getJSONObject("payload"));
                break;
            case "PositionUpdate":
                handlePositionUpdate(json.getJSONObject("payload"));
            default:
                eventLogger.warn("Unhandled event: {}", event);
        }
    }

    private void handlePrintStarted(JSONObject payload) {
        System.out.println("Payload received: " + payload.toString());

        eventLogger.info("Print started with payload: {}", payload);

        String fileName = payload.optString("name", "UNKNOWN_FILE");

        if (fileName.equals("UNKNOWN_FILE")) {
            System.err.println("File name not found in payload!");
            eventLogger.warn("File name not found in PrintStarted event!");
        } else {
            System.out.println("Extracted file name: " + fileName);
            eventLogger.info("Extracted file name: {}", fileName);
        }

        jobService.startJob(fileName);
    }

    private void handlePrintDone(JSONObject payload) {
        Job finishedJob = jobService.updateJobStatus(JobStatus.FINISHED, payload);

        if (finishedJob != null) {
            timelapseService.generateTimelapse(finishedJob.getId().toString());
            eventLogger.info("Timelapse generation started for Job ID: {}", finishedJob.getId());
        } else {
            eventLogger.warn("No active job found for timelapse generation.");
        }
    }

    private void handlePrintFailed(JSONObject payload) {
        eventLogger.warn("Print failed: {}", payload);
        jobService.updateJobStatus(JobStatus.FAILED, payload);
    }


    private void handlePrintPaused(JSONObject payload) {
        eventLogger.info("Print paused: {}", payload);
        jobService.updateJobStatus(JobStatus.PAUSED, payload);

        if (pausePhotoOnPrintPause) {
            eventLogger.info("Photo capturing paused due to print pause (Config enabled).");
            photoCaptureService.pauseCapturing();
        } else {
            eventLogger.info("Print paused, but photo capturing continues (Config disabled).");
        }
    }

    private void handlePrintResumed(JSONObject payload) {
        eventLogger.info("Print resumed: {}", payload);
        jobService.updateJobStatus(JobStatus.RESUMED, payload);

        if (pausePhotoOnPrintPause) {
            eventLogger.info("Photo capturing resumed after print resume (Config enabled).");
            photoCaptureService.resumeCapturing();
        } else {
            eventLogger.info("Print resumed, but photo capturing was never paused (Config disabled).");
        }
    }

    private void handlePrintCancelled(JSONObject payload) {
        eventLogger.warn("Print cancelled: {}", payload);
        if (errorDetectionService.isCanceledByTwin()){

        }else {
            jobService.updateJobStatus(JobStatus.CANCELLED, payload);
        }
    }

    private void handleStartup() {
        eventLogger.info("System startup detected.");
    }

    private void handleShutdown() {
        eventLogger.warn("System shutdown detected.");
    }

    private void handleClientOpened(JSONObject payload) {
        eventLogger.info("Client connected: {}", payload);
    }

    private void handleClientClosed(JSONObject payload) {
        eventLogger.warn("Client disconnected: {}", payload);
    }

    private void handleClientAuthed(JSONObject payload) {
        eventLogger.info("Client authenticated: {}", payload);
    }

    private void handleUserLoggedIn(JSONObject payload) {
        eventLogger.info("User logged in: {}", payload);
    }

    private void handleUserLoggedOut(JSONObject payload) {
        eventLogger.info("User logged out: {}", payload);
    }

    private void handleFileAdded(JSONObject payload) {
        eventLogger.info("File added: {}", payload);
    }

    private void handleFileMoved(JSONObject payload) {
        eventLogger.info("File moved: {}", payload);
    }

    private void handleFileRemoved(JSONObject payload) {
        eventLogger.warn("File removed: {}", payload);
    }

    private void handleFolderAdded(JSONObject payload) {
        eventLogger.info("Folder added: {}", payload);
    }

    private void handleFolderRemoved(JSONObject payload) {
        eventLogger.warn("Folder removed: {}", payload);
    }

    private void handleCaptureStart(JSONObject payload) {
        eventLogger.info("Timelapse capture started: {}", payload);
    }

    private void handleCaptureDone(JSONObject payload) {
        eventLogger.info("Timelapse capture done: {}", payload);
    }

    private void handleCaptureFailed(JSONObject payload) {
        eventLogger.warn("Timelapse capture failed: {}", payload);
    }

    private void handleMovieRendering(JSONObject payload) {
        eventLogger.info("Movie rendering started: {}", payload);
    }

    private void handleMovieDone(JSONObject payload) {
        eventLogger.info("Movie rendering done: {}", payload);
    }

    private void handleMovieFailed(JSONObject payload) {
        eventLogger.warn("Movie rendering failed: {}", payload);
    }

    private void handlePrinterConnected(JSONObject payload) {
        eventLogger.info("Printer connected: {}", payload);
    }

    private void handlePositionUpdate(JSONObject payload) {
        double x = payload.getDouble("x");
        double y = payload.getDouble("y");
        double z = payload.getDouble("z");

        eventLogger.info("Received Position Update - X: {}, Y: {}, Z: {}", x, y, z);

        // Forward position to ErrorDetectionService
        errorDetectionService.updateCurrentPosition(x, y, z);
    }

    private void updateJobStatus(JobStatus status) {
        Optional<Job> latestJob = jobRepository.findTopByOrderByStartTimeDesc();
        if (latestJob.isPresent()) {
            Job job = latestJob.get();
            job.setStatus(status);
            if (status == JobStatus.FINISHED || status == JobStatus.CANCELLED || status == JobStatus.FAILED) {
                job.setEndTime(LocalDateTime.now());
            }
            jobRepository.save(job);
        } else {
            eventLogger.warn("No active print job found to update.");
        }
    }
}
