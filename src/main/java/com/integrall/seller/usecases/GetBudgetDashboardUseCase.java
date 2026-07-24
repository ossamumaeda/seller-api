package com.integrall.seller.usecases;

import com.integrall.seller.dto.BudgetDashboardResponse;
import com.integrall.seller.dto.BudgetHealthStatus;
import com.integrall.seller.dto.CompetenceBudgetResponse;
import com.integrall.seller.dto.SellerBudgetSummaryResponse;
import com.integrall.seller.entity.Budget;
import com.integrall.seller.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetBudgetDashboardUseCase {

        private final BudgetRepository budgetRepository;

        public BudgetDashboardResponse execute() {

                List<Budget> budgets = budgetRepository.findAllByOrderByCompetenceDescSellerNameAsc();

                Map<LocalDate, List<Budget>> grouped = budgets.stream()
                                .collect(Collectors.groupingBy(
                                                Budget::getCompetence));

                List<CompetenceBudgetResponse> competences = grouped.entrySet()
                                .stream()
                                .sorted(Map.Entry.<LocalDate, List<Budget>>comparingByKey().reversed())
                                .map(this::mapCompetence)
                                .toList();

                return new BudgetDashboardResponse(competences);
        }

        private CompetenceBudgetResponse mapCompetence(
                        Map.Entry<LocalDate, List<Budget>> entry) {

                List<Budget> budgets = entry.getValue();

                BigDecimal totalLimit = budgets.stream()
                                .map(Budget::getLimitAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal totalBalance = budgets.stream()
                                .map(Budget::getBalance)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal usagePercentage = calculateUsage(totalLimit, totalBalance);

                List<SellerBudgetSummaryResponse> sellers = budgets.stream()
                                .map(this::mapSeller)
                                .toList();

                int critical = (int) sellers.stream()
                                .filter(s -> s.health() == BudgetHealthStatus.CRITICAL)
                                .count();

                return new CompetenceBudgetResponse(
                                entry.getKey(),
                                totalLimit,
                                totalBalance,
                                usagePercentage,
                                critical,
                                sellers);
        }

        private SellerBudgetSummaryResponse mapSeller(Budget budget) {

                BigDecimal usage = calculateUsage(
                                budget.getLimitAmount(),
                                budget.getBalance());

                return new SellerBudgetSummaryResponse(

                                budget.getSeller().getId(),

                                budget.getSeller().getName(),

                                budget.getLimitAmount(),

                                budget.getBalance(),

                                usage,

                                calculateHealth(usage)

                );

        }

        private BigDecimal calculateUsage(
                        BigDecimal limit,
                        BigDecimal balance) {

                if (limit.signum() == 0) {
                        return BigDecimal.ZERO;
                }

                BigDecimal consumed = limit.subtract(balance);

                return consumed
                                .multiply(BigDecimal.valueOf(100))
                                .divide(limit, 2, RoundingMode.HALF_UP);
        }

        private BudgetHealthStatus calculateHealth(
                        BigDecimal usage) {

                if (usage.compareTo(BigDecimal.valueOf(90)) >= 0) {
                        return BudgetHealthStatus.CRITICAL;
                }

                if (usage.compareTo(BigDecimal.valueOf(70)) >= 0) {
                        return BudgetHealthStatus.WARNING;
                }

                return BudgetHealthStatus.HEALTHY;
        }

}