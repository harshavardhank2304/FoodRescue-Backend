package com.foodrescue.backend.controller;

import com.foodrescue.backend.entity.VolunteerTask;
import com.foodrescue.backend.service.VolunteerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/volunteers")
public class VolunteerController {

    private final VolunteerService volunteerService;

    public VolunteerController(VolunteerService volunteerService) {
        this.volunteerService = volunteerService;
    }

    @GetMapping("/tasks")
    @PreAuthorize("hasAnyRole('VOLUNTEER', 'ADMIN')")
    public ResponseEntity<List<VolunteerTask>> getTasks(@AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        List<VolunteerTask> tasks = isAdmin
                ? volunteerService.getAllTasks()
                : volunteerService.getTasksForVolunteer(userDetails.getUsername());
        return ResponseEntity.ok(tasks);
    }

    @PutMapping("/tasks/{id}/status")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerTask> updateTaskStatus(@PathVariable Long id,
                                                          @RequestBody Map<String, String> body,
                                                          @AuthenticationPrincipal UserDetails userDetails) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            throw new IllegalArgumentException("Status is required in request body");
        }
        VolunteerTask updated = volunteerService.updateTaskStatus(id, status, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }

    @PutMapping("/tasks/{id}/assign")
    @PreAuthorize("hasRole('VOLUNTEER')")
    public ResponseEntity<VolunteerTask> assignToSelf(@PathVariable Long id,
                                                      @AuthenticationPrincipal UserDetails userDetails) {
        VolunteerTask task = volunteerService.assignVolunteer(id, userDetails.getUsername());
        return ResponseEntity.ok(task);
    }
}
