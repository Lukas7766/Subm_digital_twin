package com.digitwin.digitaltwin.controller;

import com.digitwin.digitaltwin.service.DigitalTwinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/digital-twin")
public class DigitalTwinController {

    @Autowired
    private DigitalTwinService digitalTwinService;

    @PostMapping("/maintain-temperature")
    public ResponseEntity<String> maintainTemperature(
            @RequestParam double targetTemperature,
            @RequestParam(defaultValue = "1.0") double tolerance) {
        new Thread(() -> digitalTwinService.maintainNozzleTemperature(targetTemperature, tolerance)).start();
        return ResponseEntity.ok("Temperature maintenance started for " + targetTemperature + "°C with tolerance " + tolerance + "°C");
    }
}
