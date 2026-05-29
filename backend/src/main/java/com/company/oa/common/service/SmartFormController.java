package com.company.oa.common.service;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/smart-form")
public class SmartFormController {
    private final SmartFormService smartFormService;

    public SmartFormController(SmartFormService smartFormService) {
        this.smartFormService = smartFormService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/suppliers")
    public List<SmartFormService.SupplierSuggestion> getSupplierSuggestions(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        return smartFormService.getSupplierSuggestions(userId, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/categories")
    public List<String> getCategorySuggestions(
            @RequestParam Long userId,
            @RequestParam(required = false) String keyword) {
        return smartFormService.getExpenseCategorySuggestions(userId, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/templates")
    public List<SmartFormService.ExpenseTemplate> getTemplates(@RequestParam Long userId) {
        return smartFormService.getExpenseTemplates(userId);
    }
}
