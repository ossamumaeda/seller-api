package com.integrall.seller.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.SalesOrder;

public interface BudgetMovementRepository extends JpaRepository<BudgetMovement, UUID> {

    Optional<BudgetMovement> findByOrderAndMovementType(
            SalesOrder order,
            BudgetMovementType movementType);

    boolean existsByOrderAndMovementType(
            SalesOrder order,
            BudgetMovementType movementType);

}