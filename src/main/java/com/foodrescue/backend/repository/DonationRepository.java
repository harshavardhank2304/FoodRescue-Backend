package com.foodrescue.backend.repository;

import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.enums.DonationStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DonationRepository extends JpaRepository<FoodDonation, Long> {

    List<FoodDonation> findByDonorId(Long donorId);

    Page<FoodDonation> findByStatus(DonationStatus status, Pageable pageable);

    Page<FoodDonation> findAll(Pageable pageable);

    long countByStatus(DonationStatus status);
}
