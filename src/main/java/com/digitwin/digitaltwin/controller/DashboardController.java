package com.digitwin.digitaltwin.controller;

import com.digitwin.digitaltwin.model.Job;
import com.digitwin.digitaltwin.model.RatingRequest;
import com.digitwin.digitaltwin.service.DashboardService;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/dashboard")
@CrossOrigin(origins = "*", allowedHeaders = "*", methods = {RequestMethod.GET, RequestMethod.POST})
public class DashboardController {

    @Autowired
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/printer-status")
    public ResponseEntity<String> getPrinterStatus() {
        JSONObject printerStatus = dashboardService.getCurrentPrinterStatus();
        return ResponseEntity.ok(printerStatus.toString());
    }

    @GetMapping("/jobs")
    public ResponseEntity<String> getAllJobs() {
        JSONArray jobs = dashboardService.getAllJobs();
        return ResponseEntity.ok(jobs.toString());
    }

    @PostMapping("/pause")
    public ResponseEntity<String> pausePrint() {
        return dashboardService.pausePrint();
    }

    @PostMapping("/preheat")
    public ResponseEntity<String> preheatNozzle() {
        return dashboardService.preheatNozzle();
    }

    @PostMapping("/continue")
    public ResponseEntity<String> continuePrint() {
        return dashboardService.continuePrint();
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelPrint() {
        return dashboardService.cancelPrint();
    }

    @PostMapping("/rate-job")
    public ResponseEntity<String> rateJob(@RequestBody RatingRequest ratingRequest) {
        return dashboardService.rateJob(ratingRequest);
    }

    @GetMapping("/timelapse")
    public ResponseEntity<?> getTimelapse(@RequestBody TimelapseRequest request) {
        return dashboardService.getTimelapse(request.getJobId());
    }

    @GetMapping("/pictureHistory")
    public ResponseEntity<?> getPictureHistory(@RequestParam Long jobId) {
        System.err.println("Fetching picture history for Job ID: " + jobId);
        return dashboardService.getPictureHistory(jobId);
    }

    @GetMapping("/last-valid")
    public ResponseEntity<?> getLastValidGcode() {
        return dashboardService.getLastValidGcode();
    }

    @PostMapping("/next-valid")
    public ResponseEntity<?> setNextValidGcode(@RequestBody NextGcodeRequest request) {
        return dashboardService.setNextValidGcode(request.getCommand());
    }

    @PostMapping("/line")
    public void drawLine() {
        dashboardService.draw();
    }



    private static class TimelapseRequest {
        private Long jobId;

        public Long getJobId() {
            return jobId;
        }

        public void setJobId(Long jobId) {
            this.jobId = jobId;
        }
    }

    private static class NextGcodeRequest {
        private String command;

        public String getCommand() {
            return command;
        }

        public void setCommand(String command) {
            this.command = command;
        }
    }


}