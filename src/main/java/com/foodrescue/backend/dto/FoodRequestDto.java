package com.foodrescue.backend.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class FoodRequestDto {

    @NotNull(message = "Donation ID is required")
    private Long donationId;

    @Positive(message = "Requested quantity must be positive")
    @NotNull(message = "Requested quantity is required")
    private Integer requestedQuantity;

    private String message;

    public Long getDonationId() { return donationId; }
    public void setDonationId(Long donationId) { this.donationId = donationId; }

    public Integer getRequestedQuantity() { return requestedQuantity; }
    public void setRequestedQuantity(Integer requestedQuantity) { this.requestedQuantity = requestedQuantity; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
