package com.digitwin.digitaltwin.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PhotoCaptureService {
    private static final Logger logger = LogManager.getLogger(PhotoCaptureService.class);

    @Value("${logging.photo_interval}")
    private int photoInterval;

    @Value("${logging.pause_photo_on_print_pause}")
    private boolean pauseOnPrintPause;

    @Value("${logging.save_path}")
    private String baseSavePath;

    @Value("${stream_url}")
    private static String STREAM_URL;

    private boolean isCapturing = false;
    private boolean isPaused = false;
    private String currentPrintId;

    //TODO:repair this - i dont know why its broken
    public void startCapturing(String printId) {
        this.isCapturing = true;
        this.isPaused = false;
        this.currentPrintId = printId;

        String savePath = baseSavePath.replace("ID", printId);
        ensureDirectoryExists(savePath);

        logger.info("Started photo capture for print ID: {}", printId);
    }

    public void stopCapturing() {
        this.isCapturing = false;
        this.isPaused = false;
        logger.info("Stopped photo capture.");
    }

    public void pauseCapturing() {
        if (isCapturing && pauseOnPrintPause) {
            this.isPaused = true;
            logger.info("Photo capturing PAUSED due to print pause (per config).");
        }
    }

    public void resumeCapturing() {
        if (isCapturing && isPaused && pauseOnPrintPause) {
            this.isPaused = false;
            logger.info("Photo capturing RESUMED after print resumed.");
        }
    }

    @Scheduled(fixedRateString = "#{${logging.photo_interval} * 1000}")
    public void captureScreenshot() {
        if (!isCapturing || isPaused || currentPrintId == null) return;

        try {
            String saveFolder = baseSavePath.replace("ID", currentPrintId);
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filePath = saveFolder + "/" + timestamp + ".jpg";

            BufferedImage image = ImageIO.read(new URL(STREAM_URL));
            ImageIO.write(image, "jpg", new File(filePath));

            logger.info("Screenshot saved: {}", filePath);
        } catch (Exception e) {
            logger.error("Failed to capture screenshot: {}", e.getMessage());
        }
    }

    private void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("Created directory: {}", path);
        }
    }
}
