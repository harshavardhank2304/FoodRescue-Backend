package com.foodrescue.backend.controller;

import com.foodrescue.backend.dto.FoodRequestDto;
import com.foodrescue.backend.entity.FoodRequest;
import com.foodrescue.backend.service.RequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/requests")
public class RequestController {

    private final RequestService requestService;

    public RequestController(RequestService requestService) {
        this.requestService = requestService;
    }

    @PostMapping
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<FoodRequest> createRequest(@Valid @RequestBody FoodRequestDto dto,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        FoodRequest request = requestService.createRequest(dto, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(request);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('NGO')")
    public ResponseEntity<List<FoodRequest>> getMyRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(requestService.getMyRequests(userDetails.getUsername()));
    }

    @GetMapping("/donor")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<List<FoodRequest>> getDonorRequests(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(requestService.getDonorRequests(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodRequest> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(requestService.getRequestById(id));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodRequest> acceptRequest(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(requestService.acceptRequest(id, userDetails.getUsername()));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodRequest> rejectRequest(@PathVariable Long id,
                                                     @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(requestService.rejectRequest(id, userDetails.getUsername()));
    }
}
