package com.foodrescue.backend.service;

import com.foodrescue.backend.dto.FoodRequestDto;
import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.FoodRequest;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.entity.VolunteerTask;
import com.foodrescue.backend.enums.DonationStatus;
import com.foodrescue.backend.enums.RequestStatus;
import com.foodrescue.backend.enums.Role;
import com.foodrescue.backend.enums.TaskStatus;
import com.foodrescue.backend.exception.ResourceNotFoundException;
import com.foodrescue.backend.repository.DonationRepository;
import com.foodrescue.backend.repository.RequestRepository;
import com.foodrescue.backend.repository.UserRepository;
import com.foodrescue.backend.repository.VolunteerTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class RequestService {

    private static final Logger log = LoggerFactory.getLogger(RequestService.class);

    private final RequestRepository requestRepository;
    private final DonationRepository donationRepository;
    private final UserRepository userRepository;
    private final VolunteerTaskRepository volunteerTaskRepository;

    public RequestService(RequestRepository requestRepository,
                          DonationRepository donationRepository,
                          UserRepository userRepository,
                          VolunteerTaskRepository volunteerTaskRepository) {
        this.requestRepository = requestRepository;
        this.donationRepository = donationRepository;
        this.userRepository = userRepository;
        this.volunteerTaskRepository = volunteerTaskRepository;
    }

    @Transactional
    public FoodRequest createRequest(FoodRequestDto dto, String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));

        if (requester.getRole() != Role.NGO) {
            throw new IllegalArgumentException("Only NGOs can request food donations");
        }

        if (dto.getRequestedQuantity() == null || dto.getRequestedQuantity() <= 0) {
            throw new IllegalArgumentException("Requested quantity must be greater than 0");
        }

        FoodDonation donation = donationRepository.findById(dto.getDonationId())
                .orElseThrow(() -> new ResourceNotFoundException("FoodDonation", dto.getDonationId()));

        if (donation.getStatus() != DonationStatus.AVAILABLE) {
            throw new IllegalStateException("Donation is not available for requests");
        }

        if (donation.getAvailableUntil() != null && donation.getAvailableUntil().isBefore(LocalDateTime.now())) {
            donation.setStatus(DonationStatus.EXPIRED);
            donationRepository.save(donation);
            throw new IllegalStateException("Donation has expired and cannot be requested");
        }

        if (dto.getRequestedQuantity() > donation.getQuantity()) {
            throw new IllegalArgumentException("Requested quantity (" + dto.getRequestedQuantity() +
                    ") exceeds available quantity (" + donation.getQuantity() + ")");
        }

        FoodRequest request = new FoodRequest();
        request.setDonation(donation);
        request.setRequester(requester);
        request.setRequestedQuantity(dto.getRequestedQuantity());
        request.setMessage(dto.getMessage());
        request.setStatus(RequestStatus.PENDING);

        donation.setStatus(DonationStatus.REQUESTED);
        donationRepository.save(donation);

        FoodRequest saved = requestRepository.save(request);
        log.info("Request created: id={} for donation={} by {}", saved.getId(), donation.getId(), requesterEmail);
        return saved;
    }

    public List<FoodRequest> getMyRequests(String requesterEmail) {
        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + requesterEmail));
        return requestRepository.findByRequesterId(requester.getId());
    }

    public FoodRequest getRequestById(Long id) {
        return requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("FoodRequest", id));
    }

    @Transactional
    public FoodRequest acceptRequest(Long id, String donorEmail) {
        FoodRequest request = getRequestById(id);

        if (!request.getDonation().getDonor().getEmail().equals(donorEmail)) {
            throw new IllegalArgumentException("Only the donor can accept this request");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING state");
        }

        request.setStatus(RequestStatus.ACCEPTED);
        request.getDonation().setStatus(DonationStatus.ACCEPTED);
        donationRepository.save(request.getDonation());

        // Auto-create unassigned volunteer task so any volunteer can claim it
        VolunteerTask task = new VolunteerTask();
        task.setRequest(request);
        task.setVolunteer(null);
        task.setStatus(TaskStatus.ASSIGNED);
        volunteerTaskRepository.save(task);

        log.info("Request accepted: id={}, volunteer task auto-created", id);
        return requestRepository.save(request);
    }

    @Transactional
    public FoodRequest rejectRequest(Long id, String donorEmail) {
        FoodRequest request = getRequestById(id);

        if (!request.getDonation().getDonor().getEmail().equals(donorEmail)) {
            throw new IllegalArgumentException("Only the donor can reject this request");
        }
        if (request.getStatus() != RequestStatus.PENDING) {
            throw new IllegalStateException("Request is not in PENDING state");
        }

        request.setStatus(RequestStatus.REJECTED);
        request.getDonation().setStatus(DonationStatus.AVAILABLE);
        donationRepository.save(request.getDonation());

        log.info("Request rejected: id={}", id);
        return requestRepository.save(request);
    }

    public List<FoodRequest> getDonorRequests(String donorEmail) {
        User donor = userRepository.findByEmail(donorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + donorEmail));
        return requestRepository.findByDonationDonorId(donor.getId());
    }
}
