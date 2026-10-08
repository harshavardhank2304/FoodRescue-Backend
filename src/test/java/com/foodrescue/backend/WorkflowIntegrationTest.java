package com.foodrescue.backend;

import com.foodrescue.backend.dto.*;
import com.foodrescue.backend.entity.*;
import com.foodrescue.backend.enums.*;
import com.foodrescue.backend.service.*;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WorkflowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthService authService;

    @Autowired
    private DonationService donationService;

    @Autowired
    private RequestService requestService;

    @Autowired
    private VolunteerService volunteerService;

    private static Long donationId;
    private static Long requestId;
    private static Long taskId;
    private static String donorToken;

    // Test 1 — Authentication: Register -> Login -> JWT received
    @Test
    @Order(1)
    void test1_authentication_registerAndLogin() {
        // Register Donor
        RegisterRequest donorReq = new RegisterRequest();
        donorReq.setName("Chef John");
        donorReq.setEmail("donor_flow@example.com");
        donorReq.setPassword("password123");
        donorReq.setRole(Role.DONOR);
        AuthResponse donorAuth = authService.register(donorReq);
        assertThat(donorAuth.getToken()).isNotEmpty();
        donorToken = donorAuth.getToken();

        // Register NGO
        RegisterRequest ngoReq = new RegisterRequest();
        ngoReq.setName("Food For All");
        ngoReq.setEmail("ngo_flow@example.com");
        ngoReq.setPassword("password123");
        ngoReq.setRole(Role.NGO);
        AuthResponse ngoAuth = authService.register(ngoReq);
        assertThat(ngoAuth.getToken()).isNotEmpty();

        // Register Volunteer
        RegisterRequest volReq = new RegisterRequest();
        volReq.setName("Speedy Courier");
        volReq.setEmail("vol_flow@example.com");
        volReq.setPassword("password123");
        volReq.setRole(Role.VOLUNTEER);
        AuthResponse volAuth = authService.register(volReq);
        assertThat(volAuth.getToken()).isNotEmpty();

        // Login Donor
        LoginRequest login = new LoginRequest();
        login.setEmail("donor_flow@example.com");
        login.setPassword("password123");
        AuthResponse loggedIn = authService.login(login);
        assertThat(loggedIn.getToken()).isNotEmpty();
        assertThat(loggedIn.getRole()).isEqualTo("DONOR");
    }

    // Test 2 — Donation: DONOR -> Create Donation -> Donation appears
    @Test
    @Order(2)
    void test2_donor_createDonation() {
        DonationRequest req = new DonationRequest();
        req.setFoodName("Vegetable Soup");
        req.setFoodCategory("Prepared Meals");
        req.setQuantity(20);
        req.setPickupAddress("456 Market St");
        req.setAvailableUntil(LocalDateTime.now().plusDays(1));
        req.setDescription("Hot nutritious soup");

        FoodDonation donation = donationService.createDonation(req, "donor_flow@example.com");
        assertThat(donation.getId()).isNotNull();
        assertThat(donation.getStatus()).isEqualTo(DonationStatus.AVAILABLE);
        donationId = donation.getId();

        // Verify donation appears in donor's donations
        List<FoodDonation> myDonations = donationService.getMyDonations("donor_flow@example.com");
        assertThat(myDonations).extracting(FoodDonation::getId).contains(donationId);
    }

    // Test 3 — NGO Request: NGO -> Request food -> DONOR sees request (and excess qty rejected)
    @Test
    @Order(3)
    void test3_ngo_requestFood() {
        // Attempt request with excess quantity (exceeds available 20)
        FoodRequestDto excessDto = new FoodRequestDto();
        excessDto.setDonationId(donationId);
        excessDto.setRequestedQuantity(50);
        assertThatThrownBy(() -> requestService.createRequest(excessDto, "ngo_flow@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("exceeds available quantity");

        // Valid request with quantity 15
        FoodRequestDto validDto = new FoodRequestDto();
        validDto.setDonationId(donationId);
        validDto.setRequestedQuantity(15);
        validDto.setMessage("Needed for community shelter");

        FoodRequest createdReq = requestService.createRequest(validDto, "ngo_flow@example.com");
        assertThat(createdReq.getId()).isNotNull();
        assertThat(createdReq.getStatus()).isEqualTo(RequestStatus.PENDING);
        requestId = createdReq.getId();

        // Verify DONOR sees incoming request
        List<FoodRequest> donorRequests = requestService.getDonorRequests("donor_flow@example.com");
        assertThat(donorRequests).extracting(FoodRequest::getId).contains(requestId);
    }

    // Test 4 — Accept + Volunteer: DONOR accepts -> Volunteer task created -> Volunteer can see/claim task
    @Test
    @Order(4)
    void test4_acceptAndVolunteerClaim() {
        // Donor accepts the request
        FoodRequest accepted = requestService.acceptRequest(requestId, "donor_flow@example.com");
        assertThat(accepted.getStatus()).isEqualTo(RequestStatus.ACCEPTED);
        assertThat(accepted.getDonation().getStatus()).isEqualTo(DonationStatus.ACCEPTED);

        // Volunteer views available tasks (sees unassigned task)
        List<VolunteerTask> availableTasks = volunteerService.getTasksForVolunteer("vol_flow@example.com");
        assertThat(availableTasks).isNotEmpty();
        VolunteerTask unassignedTask = availableTasks.stream()
                .filter(t -> t.getRequest().getId().equals(requestId))
                .findFirst()
                .orElseThrow();
        assertThat(unassignedTask.getVolunteer()).isNull();
        taskId = unassignedTask.getId();

        // Volunteer claims the task
        VolunteerTask claimed = volunteerService.assignVolunteer(taskId, "vol_flow@example.com");
        assertThat(claimed.getVolunteer()).isNotNull();
        assertThat(claimed.getVolunteer().getEmail()).isEqualTo("vol_flow@example.com");

        // Attempt second claim should fail
        assertThatThrownBy(() -> volunteerService.assignVolunteer(taskId, "vol_flow@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already assigned");
    }

    // Test 5 — Delivery: Volunteer claims -> PICKED_UP -> DELIVERED (and direct skip to DELIVERED rejected)
    @Test
    @Order(5)
    void test5_deliveryLifecycle() {
        // Cannot skip directly from ASSIGNED to DELIVERED
        assertThatThrownBy(() -> volunteerService.updateTaskStatus(taskId, "DELIVERED", "vol_flow@example.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("must be in PICKED_UP status");

        // Move to PICKED_UP
        VolunteerTask pickedUp = volunteerService.updateTaskStatus(taskId, "PICKED_UP", "vol_flow@example.com");
        assertThat(pickedUp.getStatus()).isEqualTo(TaskStatus.PICKED_UP);
        assertThat(pickedUp.getPickupTime()).isNotNull();
        assertThat(pickedUp.getRequest().getDonation().getStatus()).isEqualTo(DonationStatus.PICKED_UP);

        // Move to DELIVERED
        VolunteerTask delivered = volunteerService.updateTaskStatus(taskId, "DELIVERED", "vol_flow@example.com");
        assertThat(delivered.getStatus()).isEqualTo(TaskStatus.DELIVERED);
        assertThat(delivered.getDeliveryTime()).isNotNull();
        assertThat(delivered.getRequest().getDonation().getStatus()).isEqualTo(DonationStatus.DELIVERED);
    }

    // Test 6 — Serialization: GET /api/donations must return 200, no Jackson error, no password field
    @Test
    @Order(6)
    void test6_getDonations_noSerializationError() throws Exception {
        mockMvc.perform(get("/api/donations")
                .header("Authorization", "Bearer " + donorToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                // Password must NOT appear in any donation JSON response
                .andExpect(jsonPath("$.content[0].donor.password").doesNotExist());
    }
}
