package com.integrall.seller.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

        @ExceptionHandler(OrderNotFoundException.class)
        public ResponseEntity<ApiError> handleOrderNotFound(
                        OrderNotFoundException ex) {

                ApiError error = new ApiError(
                                ErrorCode.ORDER_NOT_FOUND.name(),
                                String.format(
                                                "Order '%s' was not found.",
                                                ex.getOrderId()));

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(BudgetNotFoundException.class)
        public ResponseEntity<ApiError> handleBudgetNotFound(
                        BudgetNotFoundException ex) {

                ApiError error = new ApiError(
                                ErrorCode.BUDGET_NOT_FOUND.name(),
                                String.format(
                                                "Budget for seller '%s' and competence '%s' was not found.",
                                                ex.getSellerId(),
                                                ex.getCompetence()));

                return ResponseEntity
                                .status(HttpStatus.NOT_FOUND)
                                .body(error);
        }

        @ExceptionHandler(InsufficientBudgetException.class)
        public ResponseEntity<ApiError> handleInsufficientBudget(
                        InsufficientBudgetException ex) {

                ApiError error = new ApiError(
                                ErrorCode.INSUFFICIENT_BUDGET.name(),
                                String.format(
                                                "Available balance %s is lower than requested amount %s.",
                                                ex.getBalance(),
                                                ex.getRequested()));

                return ResponseEntity
                                .status(HttpStatus.CONFLICT)
                                .body(error);
        }

        @ExceptionHandler(Exception.class)
        public ResponseEntity<ApiError> handleUnexpected(Exception ex) {

                ApiError error = new ApiError(
                                "INTERNAL_ERROR",
                                "An unexpected error occurred.");

                return ResponseEntity
                                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(error);
        }

}