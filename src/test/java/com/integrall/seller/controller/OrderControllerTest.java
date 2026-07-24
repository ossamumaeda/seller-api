package com.integrall.seller.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


import java.math.BigDecimal;
import java.util.UUID;


import org.junit.jupiter.api.Test;


import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import org.springframework.boot.test.mock.mockito.MockBean;

import org.springframework.test.web.servlet.MockMvc;

import com.integrall.seller.controller.OrderController;
import com.integrall.seller.exceptions.InsufficientBudgetException;
import com.integrall.seller.exceptions.OrderNotFoundException;
import com.integrall.seller.usecases.CancelOrderUseCase;
import com.integrall.seller.usecases.CloseOrderUseCase;



@WebMvcTest(OrderController.class)
class OrderControllerTest {


    @Autowired
    private MockMvc mockMvc;



    @MockBean
    private CloseOrderUseCase closeOrderUseCase;


    @MockBean
    private CancelOrderUseCase cancelOrderUseCase;



    @Test
    void shouldCloseOrderSuccessfully() throws Exception {


        UUID orderId = UUID.randomUUID();



        mockMvc.perform(
                post("/api/orders/{id}/close", orderId)
        )
        .andExpect(
                status().isNoContent()
        );



        verify(
                closeOrderUseCase
        )
        .execute(orderId);

    }



    @Test
    void shouldReturnConflictWhenBudgetIsInsufficient()
            throws Exception {


        UUID orderId = UUID.randomUUID();



        doThrow(
                new InsufficientBudgetException(
                        new BigDecimal("10.00"),
                        new BigDecimal("100.00")
                )
        )
        .when(closeOrderUseCase)
        .execute(orderId);



        mockMvc.perform(
                post("/api/orders/{id}/close", orderId)
        )
        .andExpect(
                status().isConflict()
        );

    }



    @Test
    void shouldReturnNotFoundWhenOrderDoesNotExist()
            throws Exception {


        UUID orderId = UUID.randomUUID();



        doThrow(
                new OrderNotFoundException(orderId)
        )
        .when(closeOrderUseCase)
        .execute(orderId);



        mockMvc.perform(
                post("/api/orders/{id}/close", orderId)
        )
        .andExpect(
                status().isNotFound()
        );

    }




    @Test
    void shouldCancelOrderSuccessfully() throws Exception {


        UUID orderId = UUID.randomUUID();



        mockMvc.perform(
                post("/api/orders/{id}/cancel", orderId)
        )
        .andExpect(
                status().isNoContent()
        );



        verify(
                cancelOrderUseCase
        )
        .execute(orderId);

    }


}