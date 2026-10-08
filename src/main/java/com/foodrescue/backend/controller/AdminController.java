package com.foodrescue.backend.controller;

import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.FoodRequest;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.service.AdminService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
    }

    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/donations")
    public ResponseEntity<List<FoodDonation>> getAllDonations() {
        return ResponseEntity.ok(adminService.getAllDonations());
    }

    @GetMapping("/requests")
    public ResponseEntity<List<FoodRequest>> getAllRequests() {
        return ResponseEntity.ok(adminService.getAllRequests());
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Long>> getStatistics() {
        return ResponseEntity.ok(adminService.getStatistics());
    }

    @PostMapping("/cache/evict")
    public ResponseEntity<String> evictCache() {
        adminService.evictStatisticsCache();
        return ResponseEntity.ok("Statistics cache cleared");
    }
}
