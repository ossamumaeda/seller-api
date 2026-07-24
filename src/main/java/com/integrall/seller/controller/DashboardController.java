package com.integrall.seller.controller;


import com.integrall.seller.dto.BudgetDashboardResponse;
import com.integrall.seller.usecases.GetBudgetDashboardUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final GetBudgetDashboardUseCase useCase;

    @GetMapping("/budget")
    public BudgetDashboardResponse getDashboard() {
        return useCase.execute();
    }

}