package com.foodrescue.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "FoodRescue Backend API",
                "health", "/actuator/health",
                "endpoints", Map.of(
                        "login", "/api/auth/login",
                        "register", "/api/auth/register",
                        "donations", "/api/donations",
                        "requests", "/api/requests",
                        "volunteers", "/api/volunteers"
                )
        ));
    }

    @GetMapping("/favicon.ico")
    public ResponseEntity<Void> favicon() {
        return ResponseEntity.noContent().build();
    }
}
