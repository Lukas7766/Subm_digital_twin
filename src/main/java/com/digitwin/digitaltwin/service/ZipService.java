package com.digitwin.digitaltwin.service;

import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ZipService {
    private static final String IMAGE_FOLDER = "src/main/java/com/digitwin/digitaltwin/timelapse/";

    public Optional<File> createZipFromJobImages(Long jobId) {
        Path jobFolder = Paths.get(IMAGE_FOLDER, String.valueOf(jobId));
        File zipFile = new File(IMAGE_FOLDER, jobId + ".zip");

        // Check if the folder exists and contains images
        File folder = jobFolder.toFile();
        File[] images = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".jpg") || name.toLowerCase().endsWith(".png"));

        if (images == null || images.length == 0) {
            System.err.println("No images found for Job ID: " + jobId);
            return Optional.empty();
        }

        // Create ZIP file
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {

            for (File image : images) {
                try (FileInputStream fis = new FileInputStream(image)) {
                    ZipEntry zipEntry = new ZipEntry(image.getName());
                    zos.putNextEntry(zipEntry);

                    byte[] buffer = new byte[1024];
                    int length;
                    while ((length = fis.read(buffer)) > 0) {
                        zos.write(buffer, 0, length);
                    }
                    zos.closeEntry();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }

        return Optional.of(zipFile);
    }
}
