package com.digitwin.digitaltwin.model;

public class RatingRequest {
    private Long jobId;
    private Double printQuality;
    private Double printSpeed;
    private String feedback;

    public Long getJobId() {
        return jobId;
    }

    public void setJobId(Long jobId) {
        this.jobId = jobId;
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
}