package com.foodrescue.backend.service;

import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.FoodRequest;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.enums.DonationStatus;
import com.foodrescue.backend.enums.RequestStatus;
import com.foodrescue.backend.enums.TaskStatus;
import com.foodrescue.backend.repository.DonationRepository;
import com.foodrescue.backend.repository.RequestRepository;
import com.foodrescue.backend.repository.UserRepository;
import com.foodrescue.backend.repository.VolunteerTaskRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminService.class);

    private final UserRepository userRepository;
    private final DonationRepository donationRepository;
    private final RequestRepository requestRepository;
    private final VolunteerTaskRepository volunteerTaskRepository;

    public AdminService(UserRepository userRepository,
                        DonationRepository donationRepository,
                        RequestRepository requestRepository,
                        VolunteerTaskRepository volunteerTaskRepository) {
        this.userRepository = userRepository;
        this.donationRepository = donationRepository;
        this.requestRepository = requestRepository;
        this.volunteerTaskRepository = volunteerTaskRepository;
    }

    public List<User> getAllUsers() {
        log.info("Admin: fetching all users");
        return userRepository.findAll();
    }

    public List<FoodDonation> getAllDonations() {
        log.info("Admin: fetching all donations");
        return donationRepository.findAll();
    }

    public List<FoodRequest> getAllRequests() {
        log.info("Admin: fetching all requests");
        return requestRepository.findAll();
    }

    @Cacheable("adminStatistics")
    public Map<String, Long> getStatistics() {
        log.info("Admin: computing statistics (may be cached next time)");
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalUsers", userRepository.count());
        stats.put("totalDonations", donationRepository.count());
        stats.put("availableDonations", donationRepository.countByStatus(DonationStatus.AVAILABLE));
        stats.put("totalRequests", requestRepository.count());
        stats.put("acceptedRequests", requestRepository.countByStatus(RequestStatus.ACCEPTED));
        stats.put("deliveredDonations", donationRepository.countByStatus(DonationStatus.DELIVERED));
        stats.put("assignedTasks", volunteerTaskRepository.countByStatus(TaskStatus.ASSIGNED));
        return stats;
    }

    @CacheEvict(value = "adminStatistics", allEntries = true)
    public void evictStatisticsCache() {
        log.info("Admin statistics cache evicted");
    }
}
