package com.integrall.seller.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"seller_id", "competence"}
    )
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seller_id")
    private Seller seller;

    @Column(nullable = false)
    private LocalDate competence; // Always on day 1 of the month

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal limitAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal balance;

    /* Bussiness rules */
    public void consume(BigDecimal amount) {

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        balance = balance.subtract(amount);
    }

    public void refund(BigDecimal amount) {

        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive.");
        }

        balance = balance.add(amount);
    }

}