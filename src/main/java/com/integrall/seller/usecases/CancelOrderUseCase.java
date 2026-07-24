package com.integrall.seller.usecases;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.exceptions.BudgetNotFoundException;
import com.integrall.seller.exceptions.MovementNotFoundException;
import com.integrall.seller.exceptions.OrderNotFoundException;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CancelOrderUseCase {

    private final SalesOrderRepository orderRepository;
    private final BudgetMovementRepository movementRepository;
    private final BudgetRepository budgetRepository;

    public void execute(UUID orderId) {

        SalesOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        BudgetMovement consumption = movementRepository
                .findByOrderAndMovementType(
                        order,
                        BudgetMovementType.CONSUMPTION
                )
                .orElseThrow(() -> new MovementNotFoundException(orderId));

        Budget budget = lockBudget(consumption);

        // Idempotência: o pedido já foi estornado anteriormente.
        if (movementRepository.existsByOrderAndMovementType(
                order,
                BudgetMovementType.REVERSAL
        )) {
            return;
        }

        budget.refund(consumption.getAmount());

        movementRepository.save(
                BudgetMovement.reversal(
                        budget,
                        order,
                        consumption.getAmount()
                )
        );

        order.cancel();
    }

    private Budget lockBudget(BudgetMovement consumption) {

        Budget movementBudget = consumption.getBudget();

        return budgetRepository.findByIdForUpdate(movementBudget.getId())
                .orElseThrow(() -> new BudgetNotFoundException(
                        movementBudget.getSeller().getId(),
                        movementBudget.getCompetence()
                ));
    }

}