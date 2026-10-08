package com.foodrescue.backend.service;

import com.foodrescue.backend.dto.DonationRequest;
import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.enums.DonationStatus;
import com.foodrescue.backend.enums.Role;
import com.foodrescue.backend.exception.ResourceNotFoundException;
import com.foodrescue.backend.repository.DonationRepository;
import com.foodrescue.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DonationService {

    private static final Logger log = LoggerFactory.getLogger(DonationService.class);

    private final DonationRepository donationRepository;
    private final UserRepository userRepository;

    public DonationService(DonationRepository donationRepository, UserRepository userRepository) {
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
    }

    public FoodDonation createDonation(DonationRequest request, String donorEmail) {
        User donor = userRepository.findByEmail(donorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorEmail));

        if (donor.getRole() != Role.DONOR) {
            throw new IllegalArgumentException("Only donors can create food donations");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (request.getAvailableUntil() != null && request.getAvailableUntil().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Available until date must be in the future");
        }

        FoodDonation donation = new FoodDonation();
        donation.setDonor(donor);
        donation.setFoodName(request.getFoodName());
        donation.setFoodCategory(request.getFoodCategory());
        donation.setQuantity(request.getQuantity());
        donation.setDescription(request.getDescription());
        donation.setPickupAddress(request.getPickupAddress());
        donation.setAvailableUntil(request.getAvailableUntil());
        donation.setStatus(DonationStatus.AVAILABLE);

        FoodDonation saved = donationRepository.save(donation);
        log.info("Donation created: id={} by donor={}", saved.getId(), donorEmail);

        logDonationAsync(saved.getId(), donorEmail);
        return saved;
    }

    public Page<FoodDonation> getAllDonations(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return donationRepository.findAll(pageable);
    }

    public Page<FoodDonation> getAvailableDonations(int page, int size, String sortBy, String direction) {
        Sort sort = direction.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return donationRepository.findByStatus(DonationStatus.AVAILABLE, pageable);
    }

    public FoodDonation getDonationById(Long id) {
        return donationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodDonation", id));
    }

    public List<FoodDonation> getMyDonations(String donorEmail) {
        User donor = userRepository.findByEmail(donorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorEmail));
        return donationRepository.findByDonorId(donor.getId());
    }

    public FoodDonation updateDonation(Long id, DonationRequest request, String donorEmail) {
        FoodDonation donation = getDonationById(id);

        if (!donation.getDonor().getEmail().equals(donorEmail)) {
            throw new IllegalArgumentException("You can only edit your own donations");
        }
        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Only AVAILABLE donations can be edited");
        }

        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than 0");
        }

        if (request.getAvailableUntil() != null && request.getAvailableUntil().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Available until date must be in the future");
        }

        donation.setFoodName(request.getFoodName());
        donation.setFoodCategory(request.getFoodCategory());
        donation.setQuantity(request.getQuantity());
        donation.setDescription(request.getDescription());
        donation.setPickupAddress(request.getPickupAddress());
        donation.setAvailableUntil(request.getAvailableUntil());

        log.info("Donation updated: id={}", id);
        return donationRepository.save(donation);
    }

    public void deleteDonation(Long id, String donorEmail) {
        FoodDonation donation = getDonationById(id);

        if (!donation.getDonor().getEmail().equals(donorEmail)) {
            throw new IllegalArgumentException("You can only delete your own donations");
        }

        if (donation.getStatus() == DonationStatus.ACCEPTED ||
                donation.getStatus() == DonationStatus.PICKED_UP ||
                donation.getStatus() == DonationStatus.DELIVERED) {
            throw new IllegalStateException("Cannot delete donation that has already been accepted, picked up, or delivered");
        }

        donationRepository.delete(donation);
        log.info("Donation deleted: id={}", id);
    }

    @Async
    public void logDonationAsync(Long donationId, String donorEmail) {
        log.info("[ASYNC] Donation {} created by {} logged in background", donationId, donorEmail);
    }
}
