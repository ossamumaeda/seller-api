package com.integrall.seller.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.integrall.seller.usecases.CancelOrderUseCase;
import com.integrall.seller.usecases.CloseOrderUseCase;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final CloseOrderUseCase closeOrderUseCase;
    private final CancelOrderUseCase cancelOrderUseCase;

    @PostMapping("/{id}/close")
    public ResponseEntity<Void> close(
            @PathVariable UUID id) {
        closeOrderUseCase.execute(id);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<Void> cancel(
            @PathVariable UUID id) {

        cancelOrderUseCase.execute(id);

        return ResponseEntity.noContent().build();
    }
}