package com.company.oa.oa.expense;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/oa/expenses")
public class ExpenseController {
    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping
    public PageResponse<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) Long applicantId
    ) {
        return expenseService.list(page, size, applicantId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}")
    public Map<String, Object> detail(@PathVariable long id) {
        return expenseService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping
    public Map<String, Object> create(@Valid @RequestBody ExpenseDtos.ExpenseCreateRequest request) {
        return expenseService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody ExpenseDtos.ExpenseUpdateRequest request) {
        return expenseService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/submit")
    public Map<String, Object> submit(@PathVariable long id) {
        return expenseService.submit(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/withdraw")
    public Map<String, Object> withdraw(@PathVariable long id) {
        return expenseService.withdrawExpense(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/cancel")
    public Map<String, Object> cancel(@PathVariable long id) {
        return expenseService.cancelExpense(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/{id}/mark-paid")
    public Map<String, Object> markPaid(
            @PathVariable long id,
            @RequestBody(required = false) ExpenseDtos.ExpenseMarkPaidRequest body
    ) {
        return expenseService.markPaid(id, body);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/{id}/print")
    public Map<String, Object> print(@PathVariable long id) {
        return expenseService.printPayload(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @PostMapping("/export")
    public Map<String, Object> export(@RequestBody(required = false) Map<String, Object> filter) {
        return Map.of(
                "message", "导出任务尚未接入",
                "filter", filter == null ? Map.of() : filter
        );
    }
}
