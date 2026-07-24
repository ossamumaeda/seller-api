package com.integrall.seller.dto;


import java.math.BigDecimal;
import java.util.UUID;

public record SellerBudgetSummaryResponse(

        UUID sellerId,

        String sellerName,

        BigDecimal limitAmount,

        BigDecimal balance,

        BigDecimal usagePercentage,

        BudgetHealthStatus health

) {
}