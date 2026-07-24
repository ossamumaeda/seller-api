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

        if (movementRepository.existsByOrderAndMovementType(
                order,
                BudgetMovementType.CONSUMPTION
        )) {
                return;
        }       

        LocalDate competence = LocalDate.now().withDayOfMonth(1);

        Budget budget = budgetRepository
                .findBySellerIdAndCompetence(
                        order.getSeller().getId(),
                        competence
                )
                .orElseThrow(() -> new BudgetNotFoundException(
                        order.getSeller().getId(),
                        competence
                ));

        budget.consume(
                order.getDiscount()
        );
        
        BudgetMovement budgetMovement = BudgetMovement.consumption(
                budget,
                order
        );

        movementRepository.save(budgetMovement);

        order.close();
    }

}