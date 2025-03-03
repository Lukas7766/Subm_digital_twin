package com.digitwin.digitaltwin.service;

import com.digitwin.digitaltwin.model.GcodeFile;
import com.digitwin.digitaltwin.model.PrinterStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

@Service
public class OctoPrintService {

    @Value("${octoprint.api.url}")
    private String octoprintApiUrl;

    @Value("${octoprint.api.key}")
    private String octoprintApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    private static final Logger logger = LogManager.getLogger(OctoPrintService.class);

    private static final String GCODE_STORAGE_PATH = "src/main/java/com/digitwin/digitaltwin/gcode_files/";

    private double currentX = 0.0;
    private double currentY = 0.0;
    private double currentZ = 0.0;

    /**
     * Sends a G-code command to the printer.
     * @param gcode The G-code command to send.
     */
    public ResponseEntity<String> sendGcodeCommand(String gcode) {
        try {
            String url = octoprintApiUrl + "/printer/command";

            HttpHeaders headers = createHeaders();
            JSONObject payload = new JSONObject();
            payload.put("commands", new String[]{gcode});

            HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.status(response.getStatusCode()).body("Failed to send G-code: " + response.getBody());
            }

            return ResponseEntity.ok("G-code sent successfully: " + gcode);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Error sending G-code command: " + e.getMessage());
        }
    }

    /**
     * Fetches the current nozzle temperature.
     * @return The nozzle temperature.
     */
    public double getNozzleTemperature() {
        try {
            String url = octoprintApiUrl + "/printer";

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to fetch nozzle temperature.");
            }

            JSONObject json = new JSONObject(response.getBody());
            return json.getJSONObject("temperature").getJSONObject("tool0").getDouble("actual");

        } catch (Exception e) {
            throw new RuntimeException("Error fetching nozzle temperature: " + e.getMessage());
        }
    }

    public double getBedTemperature() {
        try {
            String url = octoprintApiUrl + "/printer";
            HttpEntity<String> request = new HttpEntity<>(createHeaders());

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject json = new JSONObject(response.getBody());
                return json.getJSONObject("temperature").getJSONObject("bed").getDouble("actual");
            }
        } catch (Exception e) {
            System.err.println("Failed to fetch bed temperature: " + e.getMessage());
        }
        return -1.0;
    }

    public PrinterStatus getPrinterStatus() {
        try {
            String url = octoprintApiUrl + "/printer";
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Api-Key", octoprintApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, request, String.class);

            if (response.getStatusCode() != HttpStatus.OK || response.getBody() == null) {
                throw new RuntimeException("Failed to fetch printer status. HTTP Status: " + response.getStatusCode());
            }

            JSONObject json = new JSONObject(response.getBody());

            // Map JSON response to PrinterStatus
            PrinterStatus printerStatus = new PrinterStatus();
            printerStatus.setTimestamp(java.time.LocalDateTime.now());

            PrinterStatus.State state = new PrinterStatus.State();
            state.setText(json.getJSONObject("state").getString("text"));
            state.setError(json.getJSONObject("state").optString("error", null));

            PrinterStatus.Flags flags = new PrinterStatus.Flags();
            flags.setOperational(json.getJSONObject("state").getJSONObject("flags").getBoolean("operational"));
            flags.setPrinting(json.getJSONObject("state").getJSONObject("flags").getBoolean("printing"));
            flags.setPaused(json.getJSONObject("state").getJSONObject("flags").getBoolean("paused"));
            flags.setCancelling(json.getJSONObject("state").getJSONObject("flags").getBoolean("cancelling"));

            state.setFlags(flags);
            printerStatus.setState(state);

            // Extract temperature data
            PrinterStatus.Temperature temperature = new PrinterStatus.Temperature();
            PrinterStatus.Bed bed = new PrinterStatus.Bed();
            bed.setActual(json.getJSONObject("temperature").getJSONObject("bed").getDouble("actual"));
            bed.setTarget(json.getJSONObject("temperature").getJSONObject("bed").getDouble("target"));

            PrinterStatus.Tool tool0 = new PrinterStatus.Tool();
            tool0.setActual(json.getJSONObject("temperature").getJSONObject("tool0").getDouble("actual"));
            tool0.setTarget(json.getJSONObject("temperature").getJSONObject("tool0").getDouble("target"));

            temperature.setBed(bed);
            temperature.setTool0(tool0);
            printerStatus.setTemperature(temperature);

            return printerStatus;

        } catch (Exception e) {
            throw new RuntimeException("Error fetching printer status: " + e.getMessage());
        }
    }

    /**
     * Fetches the current print job status.
     * @return A JSON object containing job details.
     */
    public JSONObject getJobStatus() {
        try {
            String url = octoprintApiUrl + "/job";

            HttpHeaders headers = createHeaders();
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new RuntimeException("Failed to fetch job status.");
            }

            return new JSONObject(response.getBody());

        } catch (Exception e) {
            throw new RuntimeException("Error fetching job status: " + e.getMessage());
        }
    }

    /**
     * Sends a generic command to OctoPrint (e.g., pause, resume, cancel).
     * @param command The command name.
     * @param action The action parameter (optional).
     * @return Response message from OctoPrint.
     */
    public ResponseEntity<String> sendPrinterCommand(String command, String action) {
        try {
            String url = octoprintApiUrl + "/job";

            HttpHeaders headers = createHeaders();
            String payload = action != null ?
                    String.format("{\"command\": \"%s\", \"action\": \"%s\"}", command, action) :
                    String.format("{\"command\": \"%s\"}", command);

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return ResponseEntity.ok("Command sent");
            } else {
                return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to communicate with OctoPrint.");
        }
    }

    public ResponseEntity<String> pausePrint() {
        try {
            String url = octoprintApiUrl + "/job";
            HttpHeaders headers = createHeaders();
            JSONObject payload = new JSONObject();
            payload.put("command", "pause");
            payload.put("action", "pause");

            HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Failed to pause print: {}", response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body("Failed to pause print.");
            }

            logger.info("Print successfully paused.");
            return ResponseEntity.ok("Print paused successfully.");
        } catch (Exception e) {
            logger.error("Error pausing print: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error pausing print.");
        }
    }

    public ResponseEntity<String> resumePrint() {
        try {
            String url = octoprintApiUrl + "/job";
            HttpHeaders headers = createHeaders();
            JSONObject payload = new JSONObject();
            payload.put("command", "pause");
            payload.put("action", "resume");

            HttpEntity<String> request = new HttpEntity<>(payload.toString(), headers);
            logger.info("Sending RESUME command to OctoPrint: {}", payload);

            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            if (!response.getStatusCode().is2xxSuccessful()) {
                logger.error("Failed to resume print! Response: {}", response.getBody());
                return ResponseEntity.status(response.getStatusCode()).body("Failed to resume print.");
            }

            logger.info("Print successfully resumed.");
            return ResponseEntity.ok("Print resumed successfully.");
        } catch (Exception e) {
            logger.error("Error resuming print: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("Error resuming print.");
        }
    }

    public ResponseEntity<String> cancelPrint() {
        ResponseEntity<String> result1 = sendGcodeCommand("M104 S0");
        ResponseEntity<String> result2 = sendGcodeCommand("G91");
        ResponseEntity<String> result3 = sendGcodeCommand("G1 Z10 F300");
        ResponseEntity<String> result4 = sendGcodeCommand("G90");

        if (result1.getStatusCode().is2xxSuccessful() &&
                result2.getStatusCode().is2xxSuccessful() &&
                result3.getStatusCode().is2xxSuccessful() &&
                result4.getStatusCode().is2xxSuccessful()) {
            return ResponseEntity.ok("Print canceled successfully.");
        } else {
            return ResponseEntity.internalServerError().body("Failed to cancel print.");
        }
    }

    public ResponseEntity<String> preheatNozzle(double targetTemp) {
        return sendGcodeCommand("M104 S" + targetTemp);
    }

    public Optional<Double[]> getMaxTravelLimits(GcodeFile gcodeFile) {
        if (gcodeFile != null) {
            return Optional.of(new Double[]{
                    gcodeFile.getTravelMaxX(),
                    gcodeFile.getTravelMaxY(),
                    gcodeFile.getTravelMaxZ()
            });
        }
        return Optional.empty();
    }

    public String fetchLastLegalGcodeCommand(GcodeFile gcodeFile) {
        if (gcodeFile == null) return "UNKNOWN";

        try {
            Process process = Runtime.getRuntime().exec(new String[]{
                    "sshpass", "-p", "RE6PFKQj59yCb", "ssh", "-o", "StrictHostKeyChecking=no",
                    "lukas@192.168.0.106",
                    "docker exec -it octoprint tail -n 500 /octoprint/octoprint/logs/serial.log"
            });

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String lastGcode = "UNKNOWN";
            String line;

            while ((line = reader.readLine()) != null) {
                if (line.contains("Send: ") && isGcodeInFile(gcodeFile, line.substring(line.indexOf("Send: ") + 6))) {
                    lastGcode = line.substring(line.indexOf("Send: ") + 6);
                }
                if (line.contains("Recv: ok")) {
                    return lastGcode;
                }
            }
        } catch (Exception e) {
            logger.error("Error fetching last G-code from serial.log: {}", e.getMessage());
        }
        return "UNKNOWN";
    }

    public String determineNextValidGcode(GcodeFile gcodeFile, String lastLegalGcode) {
        if (gcodeFile == null || lastLegalGcode.equals("UNKNOWN")) {
            return "UNKNOWN";
        }

        File gcodeFilePath = getGcodeFilePath(gcodeFile);
        if (!gcodeFilePath.exists()) {
            logger.error("G-code file not found: {}", gcodeFilePath.getAbsolutePath());
            return "UNKNOWN";
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(gcodeFilePath))) {
            String line;
            boolean foundLast = false;

            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(lastLegalGcode.trim())) {
                    foundLast = true;
                    continue;
                }
                if (foundLast) {  // Return the very next G-code command, whatever it is
                    return line.trim();
                }
            }
        } catch (IOException e) {
            logger.error("Error reading G-code file: {}", e.getMessage());
        }
        return "UNKNOWN";
    }


    private boolean isGcodeInFile(GcodeFile gcodeFile, String gcodeCommand) {
        File gcodeFilePath = getGcodeFilePath(gcodeFile);
        if (!gcodeFilePath.exists()) {
            return false;
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(gcodeFilePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(gcodeCommand.trim())) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.error("Error reading G-code file: {}", e.getMessage());
        }
        return false;
    }

    private File getGcodeFilePath(GcodeFile gcodeFile) {
        Path gcodeDirectory = Paths.get(GCODE_STORAGE_PATH);
        try {
            return Files.walk(gcodeDirectory)
                    .filter(Files::isRegularFile)
                    .map(Path::toFile)
                    .filter(file -> file.getName().equals(gcodeFile.getName()))
                    .findFirst()
                    .orElse(new File("UNKNOWN"));
        } catch (IOException e) {
            logger.error("Error searching for G-code file: {}", e.getMessage());
            return new File("UNKNOWN");
        }
    }

    public double getCurrentX() {
        return currentX;
    }

    public double getCurrentY() {
        return currentY;
    }

    public double getCurrentZ() {
        return currentZ;
    }

    /**
     * Creates HTTP headers for OctoPrint API requests.
     * @return HttpHeaders object with API key.
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Api-Key", octoprintApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }
}
