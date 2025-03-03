package com.digitwin.digitaltwin.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DigitalTwinService {

    @Autowired
    private OctoPrintService octoPrintService;

    public void maintainNozzleTemperature(double targetTemperature, double tolerance) {
        try {
            boolean isHeating = false;
            while (true) {
                double currentTemperature = octoPrintService.getNozzleTemperature();
                if (Math.abs(currentTemperature - targetTemperature) <= tolerance) {
                    System.out.println("Temperature stable at " + currentTemperature + "°C");
                    Thread.sleep(5000);
                    continue;
                }

                if (currentTemperature < targetTemperature - tolerance && !isHeating) {
                    octoPrintService.sendGcodeCommand("M104 S" + targetTemperature);
                    isHeating = true;
                    System.out.println("Turning on nozzle heating to reach " + targetTemperature + "°C");
                }

                if (currentTemperature > targetTemperature + tolerance && isHeating) {
                    octoPrintService.sendGcodeCommand("M104 S0");
                    isHeating = false;
                    System.out.println("Turning off nozzle heating. Current temperature: " + currentTemperature + "°C");
                }

                Thread.sleep(5000);
            }
        } catch (InterruptedException e) {
            System.err.println("Temperature maintenance interrupted: " + e.getMessage());
        }
    }
}
