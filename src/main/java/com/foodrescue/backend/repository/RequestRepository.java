package com.foodrescue.backend.repository;

import com.foodrescue.backend.entity.FoodRequest;
import com.foodrescue.backend.enums.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RequestRepository extends JpaRepository<FoodRequest, Long> {

    List<FoodRequest> findByRequesterId(Long requesterId);

    List<FoodRequest> findByDonationId(Long donationId);

    List<FoodRequest> findByDonationDonorId(Long donorId);

    long countByStatus(RequestStatus status);
}
