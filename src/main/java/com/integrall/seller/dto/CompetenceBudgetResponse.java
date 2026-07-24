package com.integrall.seller.dto;


import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CompetenceBudgetResponse(

        LocalDate competence,

        BigDecimal totalLimit,

        BigDecimal totalBalance,

        BigDecimal usagePercentage,

        Integer criticalSellers,

        List<SellerBudgetSummaryResponse> sellers

) {
}