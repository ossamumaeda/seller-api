package com.integrall.seller.exceptions;

import java.math.BigDecimal;

import lombok.Getter;

@Getter
public class InsufficientBudgetException
        extends RuntimeException {

    private final BigDecimal balance;

    private final BigDecimal requested;

    public InsufficientBudgetException(
            BigDecimal balance,
            BigDecimal requested) {

        super("Insufficient budget.");

        this.balance = balance;
        this.requested = requested;
    }

}