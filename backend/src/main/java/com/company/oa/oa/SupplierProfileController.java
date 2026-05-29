package com.company.oa.oa;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/suppliers")
public class SupplierProfileController {
    private final SupplierProfileService supplierProfileService;

    public SupplierProfileController(SupplierProfileService supplierProfileService) {
        this.supplierProfileService = supplierProfileService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/profile")
    public SupplierProfileService.SupplierProfile getProfile(@RequestParam String name) {
        return supplierProfileService.getSupplierProfile(name);
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/summaries")
    public List<SupplierProfileService.SupplierSummary> getSummaries() {
        return supplierProfileService.getSupplierSummaries();
    }
}
