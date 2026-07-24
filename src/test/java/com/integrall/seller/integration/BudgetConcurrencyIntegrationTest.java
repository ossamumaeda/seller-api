package com.integrall.seller.integration;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.junit.jupiter.Testcontainers;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.BudgetMovementType;
import com.integrall.seller.entity.OrderStatus;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.entity.Seller;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;
import com.integrall.seller.repository.SellerRepository;
import com.integrall.seller.usecases.CloseOrderUseCase;

@SpringBootTest
@Testcontainers
@ActiveProfiles("test")
class BudgetConcurrencyIntegrationTest {

        @Autowired
        private CloseOrderUseCase closeOrderUseCase;

        @Autowired
        private BudgetRepository budgetRepository;

        @Autowired
        private SalesOrderRepository orderRepository;

        @Autowired
        private SellerRepository sellerRepository;

        @Autowired
        private BudgetMovementRepository movementRepository;

        @Autowired
        private TransactionTemplate transactionTemplate;

        private UUID firstOrderId;

        private UUID secondOrderId;

        private UUID budgetId;

        @BeforeEach
        void setup() {

                Seller seller = new Seller();

                seller.setName(
                                "Seller Test");

                seller = sellerRepository.save(seller);

                Budget budget = new Budget();

                budget.setSeller(seller);

                budget.setCompetence(
                                LocalDate.now()
                                                .withDayOfMonth(1));

                budget.setLimitAmount(
                                new BigDecimal("100.00"));

                budget.setBalance(
                                new BigDecimal("100.00"));

                // budgetRepository.save(budget);
                Budget savedBudget = budgetRepository.save(budget);

                budgetId = savedBudget.getId();

                SalesOrder orderOne = new SalesOrder();

                orderOne.setSeller(seller);

                orderOne.setDiscount(
                                new BigDecimal("80.00"));

                orderOne.setStatus(
                                OrderStatus.OPEN);

                orderOne = orderRepository.save(orderOne);

                SalesOrder orderTwo = new SalesOrder();

                orderTwo.setSeller(seller);

                orderTwo.setDiscount(
                                new BigDecimal("50.00"));

                orderTwo.setStatus(
                                OrderStatus.OPEN);

                orderTwo = orderRepository.save(orderTwo);

                firstOrderId = orderOne.getId();

                secondOrderId = orderTwo.getId();

        }

        @Test
        void shouldAllowOnlyOneOrderToConsumeBudgetWhenExecutedConcurrently()
                        throws Exception {

                CountDownLatch startLatch = new CountDownLatch(1);

                ExecutorService executor = Executors.newFixedThreadPool(2);

                Callable<Void> firstRequest = () -> {

                        startLatch.await();

                        closeOrderUseCase.execute(
                                        firstOrderId);

                        return null;
                };

                Callable<Void> secondRequest = () -> {

                        startLatch.await();

                        closeOrderUseCase.execute(
                                        secondOrderId);

                        return null;
                };

                Future<Void> first = executor.submit(firstRequest);

                Future<Void> second = executor.submit(secondRequest);

                /*
                 * Libera as duas threads ao mesmo tempo.
                 */
                startLatch.countDown();

                int successfulExecutions = 0;
                int failedExecutions = 0;

                try {

                        first.get();

                        successfulExecutions++;

                } catch (ExecutionException exception) {

                        failedExecutions++;

                }

                try {

                        second.get();

                        successfulExecutions++;

                } catch (ExecutionException exception) {

                        failedExecutions++;

                }

                executor.shutdown();

                /*
                 * Apenas um pedido pode consumir a verba.
                 */
                assertEquals(
                                1,
                                successfulExecutions);

                assertEquals(
                                1,
                                failedExecutions);

                List<BudgetMovement> movements = movementRepository.findAll();

                /*
                 * Apenas um consumo deve existir.
                 */
                assertEquals(
                                1,
                                movements.size());

                assertEquals(
                                BudgetMovementType.CONSUMPTION,
                                movements.get(0).getMovementType());

                List<SalesOrder> orders = orderRepository.findAll();

                long closedOrders = orders.stream()
                                .filter(order -> order.getStatus() == OrderStatus.CLOSED)
                                .count();

                /*
                 * Apenas um pedido foi fechado.
                 */
                assertEquals(
                                1,
                                closedOrders);

                Budget budget = budgetRepository.findById(budgetId)
                                .orElseThrow();

                /*
                 * O saldo nunca pode ficar negativo.
                 */
                assertTrue(
                                budget.getBalance()
                                                .compareTo(BigDecimal.ZERO) >= 0);

                /*
                 * O saldo deve bater com o consumo realizado.
                 *
                 * Pode ser:
                 * 100 - 80 = 20
                 * ou
                 * 100 - 50 = 50
                 */
                BigDecimal expectedBalance = new BigDecimal("100.00")
                                .subtract(
                                                movements.get(0)
                                                                .getAmount());

                assertEquals(
                                expectedBalance,
                                budget.getBalance());

        }
}