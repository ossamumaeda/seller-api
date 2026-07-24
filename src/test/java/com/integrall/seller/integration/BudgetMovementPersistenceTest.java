package com.integrall.seller.integration;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import com.integrall.seller.entity.Budget;
import com.integrall.seller.entity.BudgetMovement;
import com.integrall.seller.entity.OrderStatus;
import com.integrall.seller.entity.SalesOrder;
import com.integrall.seller.entity.Seller;
import com.integrall.seller.repository.BudgetMovementRepository;
import com.integrall.seller.repository.BudgetRepository;
import com.integrall.seller.repository.SalesOrderRepository;
import com.integrall.seller.repository.SellerRepository;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class BudgetMovementPersistenceTest {


    @Autowired
    private BudgetMovementRepository movementRepository;

    @Autowired
    private BudgetRepository budgetRepository;

    @Autowired
    private SalesOrderRepository orderRepository;

    @Autowired
    private SellerRepository sellerRepository;



    private Budget budget;

    private SalesOrder order;



    @BeforeEach
    void setup() {


        Seller seller = new Seller();

        seller.setName(
                "Seller Test"
        );


        seller =
                sellerRepository.save(seller);



        budget = new Budget();

        budget.setSeller(seller);

        budget.setCompetence(
                LocalDate.now()
                        .withDayOfMonth(1)
        );

        budget.setLimitAmount(
                new BigDecimal("1000.00")
        );

        budget.setBalance(
                new BigDecimal("1000.00")
        );


        budget =
                budgetRepository.save(budget);



        order = new SalesOrder();

        order.setSeller(seller);

        order.setDiscount(
                new BigDecimal("100.00")
        );

        order.setStatus(
                OrderStatus.CLOSED
        );


        order =
                orderRepository.save(order);

    }



    @Test
    void shouldNotAllowTwoConsumptionMovementsForSameOrder() {


        BudgetMovement firstMovement =
                BudgetMovement.consumption(
                        budget,
                        order
                );


        movementRepository.saveAndFlush(
                firstMovement
        );



        BudgetMovement duplicatedMovement =
                BudgetMovement.consumption(
                        budget,
                        order
                );



        assertThrows(
                DataIntegrityViolationException.class,
                () ->
                    movementRepository.saveAndFlush(
                            duplicatedMovement
                    )
        );

    }


}