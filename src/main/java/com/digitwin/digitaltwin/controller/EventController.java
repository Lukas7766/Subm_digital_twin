package com.digitwin.digitaltwin.controller;

import com.digitwin.digitaltwin.service.EventService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events")
public class EventController {

    @Autowired
    private EventService eventService;

    @PostMapping
    public ResponseEntity<String> handleEvent(@RequestBody String eventPayload) {
        try {
            eventService.processEvent(eventPayload);
            return ResponseEntity.ok("Event processed successfully");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Failed to process event: " + e.getMessage());
        }
    }
}
