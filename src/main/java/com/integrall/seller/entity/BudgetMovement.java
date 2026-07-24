package com.integrall.seller.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "budget_movements", uniqueConstraints = @UniqueConstraint(columnNames = { "order_id", "movement_type" }))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetMovement extends BaseEntity {

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "budget_id")
        private Budget budget;

        @ManyToOne(fetch = FetchType.LAZY, optional = false)
        @JoinColumn(name = "order_id")
        private SalesOrder order;

        @Enumerated(EnumType.STRING)
        @Column(name = "movement_type", nullable = false)
        private BudgetMovementType movementType;

        @Column(nullable = false, precision = 12, scale = 2)
        private BigDecimal amount;

        @Column(nullable = false, updatable = false)
        private LocalDateTime createdAt;

        @PrePersist
        void prePersist() { // Auto set to the today date
                createdAt = LocalDateTime.now();
        }

        public static BudgetMovement consumption(Budget budget, SalesOrder order) {

                BudgetMovement movement = new BudgetMovement();

                movement.setBudget(budget);

                movement.setOrder(order);

                movement.setMovementType(
                                BudgetMovementType.CONSUMPTION);

                movement.setAmount(
                                order.getDiscount());

                return movement;
        }

        public static BudgetMovement reversal(
                        Budget budget,
                        SalesOrder order,
                        BigDecimal amount) {

                BudgetMovement movement = new BudgetMovement();

                movement.setBudget(budget);
                movement.setOrder(order);
                movement.setMovementType(BudgetMovementType.REVERSAL);
                movement.setAmount(amount);

                return movement;
        }

        public boolean isConsumption() {
                return movementType == BudgetMovementType.CONSUMPTION;
        }

        public boolean isReversal() {
                return movementType == BudgetMovementType.REVERSAL;
        }

}
