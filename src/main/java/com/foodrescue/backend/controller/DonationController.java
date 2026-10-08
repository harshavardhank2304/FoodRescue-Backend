package com.foodrescue.backend.controller;

import com.foodrescue.backend.dto.DonationRequest;
import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.service.DonationService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/donations")
public class DonationController {

    private final DonationService donationService;

    public DonationController(DonationService donationService) {
        this.donationService = donationService;
    }

    @PostMapping
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodDonation> createDonation(@Valid @RequestBody DonationRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        FoodDonation donation = donationService.createDonation(request, userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED).body(donation);
    }

    @GetMapping
    public ResponseEntity<Page<FoodDonation>> getAllDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Page<FoodDonation> donations = donationService.getAllDonations(page, size, sortBy, direction);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/available")
    public ResponseEntity<Page<FoodDonation>> getAvailableDonations(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String direction) {
        Page<FoodDonation> donations = donationService.getAvailableDonations(page, size, sortBy, direction);
        return ResponseEntity.ok(donations);
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<List<FoodDonation>> getMyDonations(@AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(donationService.getMyDonations(userDetails.getUsername()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodDonation> getDonationById(@PathVariable Long id) {
        return ResponseEntity.ok(donationService.getDonationById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<FoodDonation> updateDonation(@PathVariable Long id,
                                                        @Valid @RequestBody DonationRequest request,
                                                        @AuthenticationPrincipal UserDetails userDetails) {
        FoodDonation updated = donationService.updateDonation(id, request, userDetails.getUsername());
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('DONOR')")
    public ResponseEntity<Void> deleteDonation(@PathVariable Long id,
                                               @AuthenticationPrincipal UserDetails userDetails) {
        donationService.deleteDonation(id, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
