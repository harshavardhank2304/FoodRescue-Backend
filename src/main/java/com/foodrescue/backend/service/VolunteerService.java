package com.foodrescue.backend.service;

import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.entity.VolunteerTask;
import com.foodrescue.backend.enums.DonationStatus;
import com.foodrescue.backend.enums.Role;
import com.foodrescue.backend.enums.TaskStatus;
import com.foodrescue.backend.exception.ResourceNotFoundException;
import com.foodrescue.backend.repository.DonationRepository;
import com.foodrescue.backend.repository.UserRepository;
import com.foodrescue.backend.repository.VolunteerTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class VolunteerService {

    private static final Logger log = LoggerFactory.getLogger(VolunteerService.class);

    private final VolunteerTaskRepository volunteerTaskRepository;
    private final UserRepository userRepository;
    private final DonationRepository donationRepository;

    public VolunteerService(VolunteerTaskRepository volunteerTaskRepository,
                            UserRepository userRepository,
                            DonationRepository donationRepository) {
        this.volunteerTaskRepository = volunteerTaskRepository;
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
    }

    public List<VolunteerTask> getTasksForVolunteer(String volunteerEmail) {
        User volunteer = userRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + volunteerEmail));
        return volunteerTaskRepository.findByVolunteerIdOrVolunteerIsNull(volunteer.getId());
    }

    public List<VolunteerTask> getAllTasks() {
        return volunteerTaskRepository.findAll();
    }

    @Transactional
    public VolunteerTask assignVolunteer(Long taskId, String volunteerEmail) {
        VolunteerTask task = volunteerTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("VolunteerTask", taskId));

        User volunteer = userRepository.findByEmail(volunteerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + volunteerEmail));

        if (volunteer.getRole() != Role.VOLUNTEER) {
            throw new IllegalArgumentException("Only volunteers can claim tasks");
        }

        if (task.getVolunteer() != null) {
            throw new IllegalStateException("Task is already assigned to another volunteer");
        }

        task.setVolunteer(volunteer);
        log.info("Volunteer {} assigned to task {}", volunteerEmail, taskId);
        return volunteerTaskRepository.save(task);
    }

    @Transactional
    public VolunteerTask updateTaskStatus(Long taskId, String newStatus, String volunteerEmail) {
        VolunteerTask task = volunteerTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("VolunteerTask", taskId));

        if (task.getVolunteer() == null || !task.getVolunteer().getEmail().equals(volunteerEmail)) {
            throw new IllegalArgumentException("You can only update tasks assigned to you");
        }

        TaskStatus targetStatus;
        try {
            targetStatus = TaskStatus.valueOf(newStatus.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid status: " + newStatus + ". Valid values: ASSIGNED, PICKED_UP, DELIVERED");
        }

        // Enforce valid state transitions: ASSIGNED -> PICKED_UP -> DELIVERED
        if (targetStatus == TaskStatus.PICKED_UP) {
            if (task.getStatus() != TaskStatus.ASSIGNED) {
                throw new IllegalStateException("Task must be in ASSIGNED status before being marked as PICKED_UP");
            }
            task.setPickupTime(LocalDateTime.now());
            FoodDonation donation = task.getRequest().getDonation();
            donation.setStatus(DonationStatus.PICKED_UP);
            donationRepository.save(donation);
        } else if (targetStatus == TaskStatus.DELIVERED) {
            if (task.getStatus() != TaskStatus.PICKED_UP) {
                throw new IllegalStateException("Task must be in PICKED_UP status before being marked as DELIVERED");
            }
            task.setDeliveryTime(LocalDateTime.now());
            FoodDonation donation = task.getRequest().getDonation();
            donation.setStatus(DonationStatus.DELIVERED);
            donationRepository.save(donation);
        } else {
            throw new IllegalStateException("Cannot revert task to status: " + targetStatus);
        }

        task.setStatus(targetStatus);
        log.info("Volunteer task {} updated to status {} by {}", taskId, targetStatus, volunteerEmail);
        return volunteerTaskRepository.save(task);
    }
}
