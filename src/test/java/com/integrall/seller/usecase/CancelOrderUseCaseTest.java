package com.integrall.seller.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.OrderStatus;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.entity.Seller;
import com.integrall.seller.exceptions.MovementNotFoundException;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;
import com.integrall.seller.usecases.CancelOrderUseCase;

class CancelOrderUseCaseTest {

        @Mock
        private SalesOrderRepository orderRepository;

        @Mock
        private BudgetMovementRepository movementRepository;

        @Mock
        private BudgetRepository budgetRepository;

        @InjectMocks
        private CancelOrderUseCase useCase;

        private UUID orderId;

        private SalesOrder order;

        private Budget budget;

        private BudgetMovement consumption;

        @BeforeEach
        void setup() {

                MockitoAnnotations.openMocks(this);

                orderId = UUID.randomUUID();

                Seller seller = new Seller();
                seller.setId(UUID.randomUUID());

                order = new SalesOrder();

                order.setId(orderId);
                order.setSeller(seller);
                order.setDiscount(
                                new BigDecimal("50.00"));
                order.setStatus(OrderStatus.CLOSED);

                budget = new Budget();

                budget.setId(UUID.randomUUID());
                budget.setSeller(seller);
                budget.setBalance(
                                new BigDecimal("50.00"));

                consumption = BudgetMovement.consumption(
                                budget,
                                order);

        }

        @Test
        void shouldRefundBudgetAndCancelOrder() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.of(consumption));

                when(
                                budgetRepository.findByIdForUpdate(
                                                budget.getId()))
                                .thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.REVERSAL))
                                .thenReturn(false);

                useCase.execute(orderId);

                assertEquals(
                                OrderStatus.CANCELLED,
                                order.getStatus());

                assertEquals(
                                new BigDecimal("100.00"),
                                budget.getBalance());

                verify(movementRepository)
                                .save(any(BudgetMovement.class));

        }

        @Test
        void shouldNotCreateReversalMovementWhenCancellationWasAlreadyProcessed() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.of(consumption));

                when(
                                budgetRepository.findByIdForUpdate(
                                                budget.getId()))
                                .thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.REVERSAL))
                                .thenReturn(true);

                useCase.execute(orderId);

                assertEquals(
                                new BigDecimal("50.00"),
                                budget.getBalance());

                assertEquals(
                                OrderStatus.CLOSED,
                                order.getStatus());

                verify(
                                movementRepository,
                                never())
                                .save(any());

        }

        @Test
        void shouldThrowExceptionWhenConsumptionMovementDoesNotExist() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.empty());

                assertThrows(
                                MovementNotFoundException.class,
                                () -> useCase.execute(orderId));

                verify(
                                budgetRepository,
                                never())
                                .findByIdForUpdate(any());

        }

        @Test
        void shouldCreateReversalMovementWithOriginalConsumptionAmount() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.of(consumption));

                when(
                                budgetRepository.findByIdForUpdate(
                                                budget.getId()))
                                .thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.REVERSAL))
                                .thenReturn(false);

                useCase.execute(orderId);

                verify(
                                movementRepository)
                                .save(argThat(movement -> movement.getMovementType() == BudgetMovementType.REVERSAL
                                                &&
                                                movement.getAmount()
                                                                .equals(
                                                                                new BigDecimal("50.00"))
                                                &&
                                                movement.getBudget() == budget));

        }

        @Test
        void shouldThrowExceptionWhenOrderDoesNotHaveConsumptionMovement() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.empty());

                assertThrows(
                                MovementNotFoundException.class,
                                () -> useCase.execute(orderId));

                verify(
                                budgetRepository,
                                never())
                                .findByIdForUpdate(any());

        }

        @Test
        void shouldNotRefundTwiceWhenAlreadyReversed() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                movementRepository.findByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(Optional.of(consumption));

                when(
                                budgetRepository.findByIdForUpdate(
                                                budget.getId()))
                                .thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.REVERSAL))
                                .thenReturn(true);

                useCase.execute(orderId);

                assertEquals(
                                new BigDecimal("50.00"),
                                budget.getBalance());

                verify(
                                movementRepository,
                                never())
                                .save(any());

        }

}