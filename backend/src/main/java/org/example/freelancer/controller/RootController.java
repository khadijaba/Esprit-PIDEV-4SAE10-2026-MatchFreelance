package org.example.freelancer.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public Map<String, String> root() {
        return Map.of(
            "application", "Freelancer API",
            "status", "running",
            "endpoints", "/api/events"
        );
    }
}
