package com.digitwin.digitaltwin.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Service;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class TimelapseService {
    private static final Logger logger = LogManager.getLogger(TimelapseService.class);
    private static final String SAVE_PATH = "src/main/java/com/digitwin/digitaltwin/timelapse/";
    private static final String TIMELAPSE_FOLDER = "src/main/java/com/digitwin/digitaltwin/timelapse";

    public void generateTimelapse(String printId) {
        try {
            String printFolder = SAVE_PATH + printId + "/";
            ensureDirectoryExists(printFolder);

            String outputFile = printFolder + printId + "_tm.mp4";

            ProcessBuilder builder = new ProcessBuilder(
                    "ffmpeg", "-framerate", "10", "-pattern_type", "glob", "-i",
                    printFolder + "*.jpg", "-c:v", "libx264", "-pix_fmt", "yuv420p", outputFile
            );
            builder.redirectErrorStream(true);
            Process process = builder.start();
            process.waitFor();

            logger.info("Timelapse generated: {}", outputFile);
        } catch (Exception e) {
            logger.error("Error generating timelapse: {}", e.getMessage());
        }
    }

    private void ensureDirectoryExists(String path) {
        File dir = new File(path);
        if (!dir.exists()) {
            dir.mkdirs();
            logger.info("Created directory: {}", path);
        }
    }

    public Optional<File> getTimelapseForJob(Long jobId) {
        Path timelapseFilePath = Paths.get(TIMELAPSE_FOLDER, "job_" + jobId + ".mp4");
        File timelapseFile = timelapseFilePath.toFile();

        if (timelapseFile.exists() && timelapseFile.isFile()) {
            return Optional.of(timelapseFile);
        } else {
            return Optional.empty();
        }
    }
}
