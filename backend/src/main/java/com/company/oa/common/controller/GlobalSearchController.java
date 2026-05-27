package com.company.oa.common.controller;

import com.company.oa.common.service.GlobalSearchService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search")
public class GlobalSearchController {
    private final GlobalSearchService globalSearchService;

    public GlobalSearchController(GlobalSearchService globalSearchService) {
        this.globalSearchService = globalSearchService;
    }

    @GetMapping
    public Map<String, List<Map<String, Object>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "5") int limit) {
        return globalSearchService.search(q, limit);
    }
}
