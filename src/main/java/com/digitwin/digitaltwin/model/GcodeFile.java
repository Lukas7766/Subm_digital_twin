package com.digitwin.digitaltwin.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "gcode_file")
public class GcodeFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String path;
    private String hash;
    private String origin;
    private Long size;
    private LocalDateTime uploadDate;
    private Double estimatedPrintTime;
    private Double filamentUsed;

    // New fields: Dimensions and travel area
    private Double width;
    private Double depth;
    private Double height;

    private Double maxX;
    private Double maxY;
    private Double maxZ;
    private Double minX;
    private Double minY;
    private Double minZ;

    private Double travelMaxX;
    private Double travelMaxY;
    private Double travelMaxZ;
    private Double travelMinX;
    private Double travelMinY;
    private Double travelMinZ;

    private Double travelDepth;
    private Double travelHeight;
    private Double travelWidth;


    private Double medianPrintTime;

    private String downloadUrl;

    @OneToMany(mappedBy = "gcodeFile")
    private java.util.List<Job> jobs;

    public GcodeFile() {}

    public GcodeFile(String name, String path, String hash, String origin, Long size, LocalDateTime uploadDate,
                     Double estimatedPrintTime, Double filamentUsed,
                     Double width, Double depth, Double height,
                     Double maxX, Double maxY, Double maxZ, Double minX, Double minY, Double minZ,
                     Double travelMaxX, Double travelMaxY, Double travelMaxZ,
                     Double travelMinX, Double travelMinY, Double travelMinZ,
                     Double travelDepth, Double travelHeight, Double travelWidth,
                     String downloadUrl) {
        this.name = name;
        this.path = path;
        this.hash = hash;
        this.origin = origin;
        this.size = size;
        this.uploadDate = uploadDate;
        this.estimatedPrintTime = estimatedPrintTime;
        this.filamentUsed = filamentUsed;

        this.width = width;
        this.depth = depth;
        this.height = height;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.travelMaxX = travelMaxX;
        this.travelMaxY = travelMaxY;
        this.travelMaxZ = travelMaxZ;
        this.travelMinX = travelMinX;
        this.travelMinY = travelMinY;
        this.travelMinZ = travelMinZ;

        this.travelDepth = travelDepth;
        this.travelHeight = travelHeight;
        this.travelWidth = travelWidth;

        this.medianPrintTime = null;  // Initially null
        this.downloadUrl = downloadUrl;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getOrigin() {
        return origin;
    }

    public void setOrigin(String origin) {
        this.origin = origin;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public Double getEstimatedPrintTime() {
        return estimatedPrintTime;
    }

    public void setEstimatedPrintTime(Double estimatedPrintTime) {
        this.estimatedPrintTime = estimatedPrintTime;
    }

    public Double getFilamentUsed() {
        return filamentUsed;
    }

    public void setFilamentUsed(Double filamentUsed) {
        this.filamentUsed = filamentUsed;
    }

    public Double getWidth() {
        return width;
    }

    public void setWidth(Double width) {
        this.width = width;
    }

    public Double getDepth() {
        return depth;
    }

    public void setDepth(Double depth) {
        this.depth = depth;
    }

    public Double getHeight() {
        return height;
    }

    public void setHeight(Double height) {
        this.height = height;
    }

    public Double getMaxX() {
        return maxX;
    }

    public void setMaxX(Double maxX) {
        this.maxX = maxX;
    }

    public Double getMaxY() {
        return maxY;
    }

    public void setMaxY(Double maxY) {
        this.maxY = maxY;
    }

    public Double getMaxZ() {
        return maxZ;
    }

    public void setMaxZ(Double maxZ) {
        this.maxZ = maxZ;
    }

    public Double getMinX() {
        return minX;
    }

    public void setMinX(Double minX) {
        this.minX = minX;
    }

    public Double getMinY() {
        return minY;
    }

    public void setMinY(Double minY) {
        this.minY = minY;
    }

    public Double getMinZ() {
        return minZ;
    }

    public void setMinZ(Double minZ) {
        this.minZ = minZ;
    }

    public Double getTravelMaxX() {
        return travelMaxX;
    }

    public void setTravelMaxX(Double travelMaxX) {
        this.travelMaxX = travelMaxX;
    }

    public Double getTravelMaxY() {
        return travelMaxY;
    }

    public void setTravelMaxY(Double travelMaxY) {
        this.travelMaxY = travelMaxY;
    }

    public Double getTravelMaxZ() {
        return travelMaxZ;
    }

    public void setTravelMaxZ(Double travelMaxZ) {
        this.travelMaxZ = travelMaxZ;
    }

    public Double getTravelMinX() {
        return travelMinX;
    }

    public void setTravelMinX(Double travelMinX) {
        this.travelMinX = travelMinX;
    }

    public Double getTravelMinY() {
        return travelMinY;
    }

    public void setTravelMinY(Double travelMinY) {
        this.travelMinY = travelMinY;
    }

    public Double getTravelMinZ() {
        return travelMinZ;
    }

    public void setTravelMinZ(Double travelMinZ) {
        this.travelMinZ = travelMinZ;
    }

    public Double getTravelDepth() {
        return travelDepth;
    }

    public void setTravelDepth(Double travelDepth) {
        this.travelDepth = travelDepth;
    }

    public Double getTravelHeight() {
        return travelHeight;
    }

    public void setTravelHeight(Double travelHeight) {
        this.travelHeight = travelHeight;
    }

    public Double getTravelWidth() {
        return travelWidth;
    }

    public void setTravelWidth(Double travelWidth) {
        this.travelWidth = travelWidth;
    }

    public Double getMedianPrintTime() {
        return medianPrintTime;
    }

    public void setMedianPrintTime(Double medianPrintTime) {
        this.medianPrintTime = medianPrintTime;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public List<Job> getJobs() {
        return jobs;
    }

    public void setJobs(List<Job> jobs) {
        this.jobs = jobs;
    }
}
