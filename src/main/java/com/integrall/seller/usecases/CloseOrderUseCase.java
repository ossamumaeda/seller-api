package com.integrall.seller.usecases;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.exceptions.BudgetNotFoundException;
import com.integrall.seller.exceptions.OrderNotFoundException;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class CloseOrderUseCase {

    private final SalesOrderRepository orderRepository;
    private final BudgetRepository budgetRepository;
    private final BudgetMovementRepository movementRepository;

    public void execute(UUID orderId) {

        SalesOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        Budget budget = lockBudget(order);

        // Idempotência: o pedido já foi fechado anteriormente.
        if (movementRepository.existsByOrderAndMovementType(
                order,
                BudgetMovementType.CONSUMPTION
        )) {
            return;
        }

        budget.consume(order.getDiscount());
        
        movementRepository.save(
                BudgetMovement.consumption(
                        budget,
                        order
                )
        );

        order.close();
    }

    private Budget lockBudget(SalesOrder order) {

        LocalDate competence = LocalDate.now().withDayOfMonth(1);

        return budgetRepository.findForUpdate(
                        order.getSeller().getId(),
                        competence
                )
                .orElseThrow(() -> new BudgetNotFoundException(
                        order.getSeller().getId(),
                        competence
                ));
    }

}