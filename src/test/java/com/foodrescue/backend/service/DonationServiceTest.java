package com.foodrescue.backend.service;

import com.foodrescue.backend.dto.DonationRequest;
import com.foodrescue.backend.entity.FoodDonation;
import com.foodrescue.backend.entity.User;
import com.foodrescue.backend.enums.DonationStatus;
import com.foodrescue.backend.enums.Role;
import com.foodrescue.backend.exception.ResourceNotFoundException;
import com.foodrescue.backend.repository.DonationRepository;
import com.foodrescue.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DonationServiceTest {

    @Mock
    private DonationRepository donationRepository;

    @Mock
    private UserRepository userRepository;

    private DonationService donationService;

    private User donor;

    @BeforeEach
    void setUp() {
        donationService = new DonationService(donationRepository, userRepository);
        donor = new User("Alice", "alice@example.com", "hashed", "9876543210", Role.DONOR);
        donor.setId(1L);
    }

    @Test
    void createDonation_shouldSaveAndReturnDonation() {
        DonationRequest request = new DonationRequest();
        request.setFoodName("Rice");
        request.setFoodCategory("Grains");
        request.setQuantity(10);
        request.setPickupAddress("123 Main St");
        request.setAvailableUntil(LocalDateTime.now().plusDays(2));

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(donor));
        when(donationRepository.save(any(FoodDonation.class))).thenAnswer(inv -> {
            FoodDonation d = inv.getArgument(0);
            d.setId(1L);
            return d;
        });

        FoodDonation result = donationService.createDonation(request, "alice@example.com");

        assertThat(result.getFoodName()).isEqualTo("Rice");
        assertThat(result.getStatus()).isEqualTo(DonationStatus.AVAILABLE);
        assertThat(result.getDonor()).isEqualTo(donor);
        verify(donationRepository).save(any(FoodDonation.class));
    }

    @Test
    void getDonationById_shouldThrowWhenNotFound() {
        when(donationRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> donationService.getDonationById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteDonation_shouldThrowWhenNotOwner() {
        FoodDonation donation = new FoodDonation();
        donation.setId(1L);
        donation.setDonor(donor);
        donation.setStatus(DonationStatus.AVAILABLE);

        when(donationRepository.findById(1L)).thenReturn(Optional.of(donation));

        assertThatThrownBy(() -> donationService.deleteDonation(1L, "other@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("own donations");
    }

    @Test
    void getAllDonations_shouldReturnPagedResults() {
        FoodDonation d1 = new FoodDonation();
        d1.setId(1L);
        d1.setFoodName("Rice");
        Page<FoodDonation> page = new PageImpl<>(List.of(d1));

        when(donationRepository.findAll(any(Pageable.class))).thenReturn(page);

        Page<FoodDonation> result = donationService.getAllDonations(0, 10, "createdAt", "desc");

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getFoodName()).isEqualTo("Rice");
    }
}
