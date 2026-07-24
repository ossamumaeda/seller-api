package com.integrall.seller.domain;

import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.exceptions.InsufficientBudgetException;

class BudgetTest {

        @Test
        void shouldConsumeBudgetWhenBalanceIsEnough() {

                Budget budget = new Budget();

                budget.setBalance(
                                new BigDecimal("100.00"));

                budget.consume(
                                new BigDecimal("30.00"));

                assertEquals(
                                new BigDecimal("70.00"),
                                budget.getBalance());
        }

        @Test
        void shouldThrowExceptionWhenBalanceIsInsufficient() {

                Budget budget = new Budget();

                budget.setBalance(
                                new BigDecimal("50.00"));

                assertThrows(
                                InsufficientBudgetException.class,
                                () -> budget.consume(
                                                new BigDecimal("100.00")));

                assertEquals(
                                new BigDecimal("50.00"),
                                budget.getBalance());
        }

        @Test
        void shouldNotAllowNegativeConsumption() {

                Budget budget = new Budget();

                budget.setBalance(
                                new BigDecimal("100.00"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> budget.consume(
                                                new BigDecimal("-10.00")));
        }

        @Test
        void shouldRefundConsumedAmount() {

                Budget budget = new Budget();

                budget.setBalance(
                                new BigDecimal("100.00"));

                budget.consume(
                                new BigDecimal("40.00"));

                budget.refund(
                                new BigDecimal("40.00"));

                assertEquals(
                                new BigDecimal("100.00"),
                                budget.getBalance());
        }

        @Test
        void shouldNotAllowZeroConsumption() {

                Budget budget = new Budget();

                budget.setBalance(
                                new BigDecimal("100.00"));

                assertThrows(
                                IllegalArgumentException.class,
                                () -> budget.consume(
                                                BigDecimal.ZERO));
        }

        @Test
        void shouldNotAllowZeroRefund() {

                Budget budget = new Budget();

                assertThrows(
                                IllegalArgumentException.class,
                                () -> budget.refund(
                                                BigDecimal.ZERO));
        }

}