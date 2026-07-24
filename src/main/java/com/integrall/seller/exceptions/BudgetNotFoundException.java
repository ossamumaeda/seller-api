package com.integrall.seller.exceptions;

import java.time.LocalDate;
import java.util.UUID;

public class BudgetNotFoundException extends RuntimeException {

    private final UUID sellerId;
    private final LocalDate competence;

    public BudgetNotFoundException(UUID sellerId, LocalDate competence) {
        super("Budget not found.");
        this.sellerId = sellerId;
        this.competence = competence;
    }

    public UUID getSellerId() {
        return sellerId;
    }

    public LocalDate getCompetence() {
        return competence;
    }
}