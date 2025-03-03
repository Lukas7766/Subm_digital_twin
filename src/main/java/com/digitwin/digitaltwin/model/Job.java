package com.digitwin.digitaltwin.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fileName;
    private Double progress;
    @Enumerated(EnumType.STRING)
    private JobStatus status;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime estimatedCompletionTime;

    private String gcodeFilePath;
    private Double filamentUsed;

    @ManyToOne
    @JoinColumn(name = "gcode_file_id")
    private GcodeFile gcodeFile;

    private Double printQuality;
    private Double printSpeed;

    @Column(length = 5000)
    private String feedback;

    public Job() {
    }

    public Job(String fileName, JobStatus status, LocalDateTime startTime) {
        this.fileName = fileName;
        this.status = status;
        this.startTime = startTime;
        this.filamentUsed = 0.0;
    }

    public Double getPrintQuality() {
        return printQuality;
    }

    public void setPrintQuality(Double printQuality) {
        this.printQuality = printQuality;
    }

    public Double getPrintSpeed() {
        return printSpeed;
    }

    public void setPrintSpeed(Double printSpeed) {
        this.printSpeed = printSpeed;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Double getProgress() {
        return progress;
    }

    public void setProgress(Double progress) {
        this.progress = progress;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public void setStatus(JobStatus status) {
        this.status = status;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public LocalDateTime getEstimatedCompletionTime() {
        return estimatedCompletionTime;
    }

    public void setEstimatedCompletionTime(LocalDateTime estimatedCompletionTime) {
        this.estimatedCompletionTime = estimatedCompletionTime;
    }

    public JobStatus getStatus() {
        return status;
    }

    public Double getFilamentUsed() {
        return filamentUsed;
    }

    public void setFilamentUsed(Double filamentUsed) {
        this.filamentUsed = filamentUsed;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getGcodeFilePath() {
        return gcodeFilePath;
    }

    public void setGcodeFilePath(String gcodeFilePath) {
        this.gcodeFilePath = gcodeFilePath;
    }

    public GcodeFile getGcodeFile() {
        return gcodeFile;
    }

    public void setGcodeFile(GcodeFile gcodeFile) {
        this.gcodeFile = gcodeFile;
    }
}

