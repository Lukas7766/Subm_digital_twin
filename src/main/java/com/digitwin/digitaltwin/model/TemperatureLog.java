package com.digitwin.digitaltwin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TemperatureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    private double tool0Temperature;
    private double bedTemperature;
    private LocalDateTime timestamp;

    public TemperatureLog() {}

    public TemperatureLog(Job job, double tool0Temperature, double bedTemperature, LocalDateTime timestamp) {
        this.job = job;
        this.tool0Temperature = tool0Temperature;
        this.bedTemperature = bedTemperature;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public Job getJob() {
        return job;
    }

    public double getTool0Temperature() {
        return tool0Temperature;
    }

    public double getBedTemperature() {
        return bedTemperature;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }
}
