package com.foodrescue.backend.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDateTime;

public class DonationRequest {

    @NotBlank(message = "Food name is required")
    private String foodName;

    @NotBlank(message = "Food category is required")
    private String foodCategory;

    @Positive(message = "Quantity must be positive")
    @NotNull(message = "Quantity is required")
    private Integer quantity;

    private String description;

    @NotBlank(message = "Pickup address is required")
    private String pickupAddress;

    @NotNull(message = "Available until date is required")
    private LocalDateTime availableUntil;

    public String getFoodName() { return foodName; }
    public void setFoodName(String foodName) { this.foodName = foodName; }

    public String getFoodCategory() { return foodCategory; }
    public void setFoodCategory(String foodCategory) { this.foodCategory = foodCategory; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPickupAddress() { return pickupAddress; }
    public void setPickupAddress(String pickupAddress) { this.pickupAddress = pickupAddress; }

    public LocalDateTime getAvailableUntil() { return availableUntil; }
    public void setAvailableUntil(LocalDateTime availableUntil) { this.availableUntil = availableUntil; }
}
