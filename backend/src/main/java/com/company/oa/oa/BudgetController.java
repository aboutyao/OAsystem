package com.company.oa.oa;

import com.company.oa.entity.oa.Budget;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/budgets")
public class BudgetController {
    private final BudgetService budgetService;

    public BudgetController(BudgetService budgetService) {
        this.budgetService = budgetService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'finance:budget:manage')")
    @PostMapping
    public Map<String, Object> create(@RequestBody Budget budget) {
        return budgetService.createBudget(budget);
    }

    @PreAuthorize("hasAnyAuthority('*', 'finance:budget:view', 'finance:budget:manage')")
    @GetMapping
    public List<Map<String, Object>> list(
            @RequestParam(required = false) Long deptId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer year) {
        return budgetService.listBudgets(deptId, category, year);
    }

    @PreAuthorize("hasAnyAuthority('*', 'finance:budget:view', 'finance:budget:manage')")
    @GetMapping("/warnings")
    public List<Map<String, Object>> warnings() {
        return budgetService.getBudgetWarnings();
    }
}
