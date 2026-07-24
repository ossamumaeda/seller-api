package com.integrall.seller.usecase;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.OrderStatus;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.entity.Seller;
import com.integrall.seller.exceptions.BudgetNotFoundException;
import com.integrall.seller.exceptions.InsufficientBudgetException;
import com.integrall.seller.exceptions.OrderNotFoundException;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;
import com.integrall.seller.usecases.CloseOrderUseCase;

class CloseOrderUseCaseTest {

        @Mock
        private SalesOrderRepository orderRepository;

        @Mock
        private BudgetRepository budgetRepository;

        @Mock
        private BudgetMovementRepository movementRepository;

        @InjectMocks
        private CloseOrderUseCase useCase;

        private UUID orderId;
        private SalesOrder order;
        private Budget budget;

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
                                new BigDecimal("30.00"));
                order.setStatus(OrderStatus.OPEN);

                budget = new Budget();
                budget.setId(UUID.randomUUID());
                budget.setSeller(seller);
                budget.setCompetence(
                                LocalDate.now().withDayOfMonth(1));
                budget.setBalance(
                                new BigDecimal("100.00"));
        }

        @Test
        void shouldCloseOrderAndConsumeBudget() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(budgetRepository.findForUpdate(
                                any(),
                                any())).thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(false);

                useCase.execute(orderId);

                assertEquals(
                                OrderStatus.CLOSED,
                                order.getStatus());

                assertEquals(
                                new BigDecimal("70.00"),
                                budget.getBalance());

                verify(movementRepository)
                                .save(any(BudgetMovement.class));
        }

        @Test
        void shouldNotCloseOrderWhenBudgetIsInsufficient() {

                order.setDiscount(
                                new BigDecimal("200.00"));

                budget.setBalance(
                                new BigDecimal("100.00"));

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(budgetRepository.findForUpdate(
                                any(),
                                any())).thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(false);

                assertThrows(
                                InsufficientBudgetException.class,
                                () -> useCase.execute(orderId));

                assertEquals(
                                OrderStatus.OPEN,
                                order.getStatus());

                assertEquals(
                                new BigDecimal("100.00"),
                                budget.getBalance());

                verify(
                                movementRepository,
                                never()).save(any());
        }

        @Test
        void shouldNotConsumeBudgetTwiceWhenMovementAlreadyExists() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(budgetRepository.findForUpdate(
                                any(),
                                any())).thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(true);

                useCase.execute(orderId);

                assertEquals(
                                new BigDecimal("100.00"),
                                budget.getBalance());

                assertEquals(
                                OrderStatus.OPEN,
                                order.getStatus());

                verify(
                                movementRepository,
                                never()).save(any());
        }

        @Test
        void shouldCreateConsumptionMovementWithOrderDiscount() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(budgetRepository.findForUpdate(
                                any(),
                                any())).thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(false);

                useCase.execute(orderId);

                ArgumentCaptor<BudgetMovement> captor = ArgumentCaptor.forClass(
                                BudgetMovement.class);

                verify(movementRepository)
                                .save(captor.capture());

                BudgetMovement movement = captor.getValue();

                assertEquals(
                                BudgetMovementType.CONSUMPTION,
                                movement.getMovementType());

                assertEquals(
                                new BigDecimal("30.00"),
                                movement.getAmount());

                assertEquals(
                                order,
                                movement.getOrder());
        }

        @Test
        void shouldThrowExceptionWhenOrderDoesNotExist() {

                UUID orderId = UUID.randomUUID();

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.empty());

                assertThrows(
                                OrderNotFoundException.class,
                                () -> useCase.execute(orderId));

                verify(
                                budgetRepository,
                                never()).findForUpdate(any(), any());

        }

        @Test
        void shouldThrowExceptionWhenBudgetDoesNotExist() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                budgetRepository.findForUpdate(any(), any()))
                                .thenReturn(Optional.empty());

                assertThrows(
                                BudgetNotFoundException.class,
                                () -> useCase.execute(orderId));

                verify(
                                movementRepository,
                                never()).save(any());

        }

        @Test
        void shouldNotCreateSecondConsumptionMovement() {

                when(orderRepository.findById(orderId))
                                .thenReturn(Optional.of(order));

                when(
                                budgetRepository.findForUpdate(any(), any()))
                                .thenReturn(Optional.of(budget));

                when(
                                movementRepository.existsByOrderAndMovementType(
                                                order,
                                                BudgetMovementType.CONSUMPTION))
                                .thenReturn(true);

                useCase.execute(orderId);

                verify(
                                movementRepository,
                                never())
                                .save(any());

                assertEquals(
                                new BigDecimal("100.00"),
                                budget.getBalance());

        }

}