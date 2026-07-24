package com.integrall.seller.exceptions;


import java.util.UUID;

public class MovementNotFoundException extends RuntimeException {

    private final UUID orderId;

    public MovementNotFoundException(UUID orderId) {
        super("Budget movement not found.");
        this.orderId = orderId;
    }

    public UUID getOrderId() {
        return orderId;
    }
}