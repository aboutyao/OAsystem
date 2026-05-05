package com.company.oa.common.module;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/modules")
public class ModuleStatusController {

    @GetMapping
    public Map<String, Object> modules() {
        return Map.of(
                "status", "SCAFFOLDED",
                "modules", List.of(
                        "auth", "system", "org", "permission", "workflow", "rule", "form",
                        "message", "file", "notice", "oa", "contract", "asset", "meeting",
                        "report", "audit", "job", "ops"
                )
        );
    }
}
