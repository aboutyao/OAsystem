package com.company.oa.rule;

import com.company.oa.common.api.PageResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class RuleController {
    private final RuleService ruleService;

    public RuleController(RuleService ruleService) {
        this.ruleService = ruleService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish', 'permission:view', 'org:view')")
    @GetMapping("/rule-groups")
    public List<Map<String, Object>> ruleGroups() {
        return ruleService.listRuleGroups();
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PostMapping("/rule-groups")
    public Map<String, Object> createRuleGroup(@Valid @RequestBody RuleDtos.RuleGroupCreateRequest request) {
        return ruleService.createRuleGroup(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PutMapping("/rule-groups/{id}")
    public Map<String, Object> updateRuleGroup(@PathVariable long id, @Valid @RequestBody RuleDtos.RuleGroupUpdateRequest request) {
        return ruleService.updateRuleGroup(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @DeleteMapping("/rule-groups/{id}")
    public Map<String, Object> deleteRuleGroup(@PathVariable long id) {
        return ruleService.deleteRuleGroup(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish', 'permission:view', 'org:view')")
    @GetMapping("/rules")
    public PageResponse<Map<String, Object>> rules(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size
    ) {
        return ruleService.listRules(page, size);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish', 'permission:view', 'org:view')")
    @GetMapping("/rules/{id}")
    public Map<String, Object> ruleDetail(@PathVariable long id) {
        return ruleService.ruleDetail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PostMapping("/rules")
    public Map<String, Object> createRule(@Valid @RequestBody RuleDtos.RuleCreateRequest request) {
        return ruleService.createRule(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PutMapping("/rules/{id}")
    public Map<String, Object> updateRule(@PathVariable long id, @Valid @RequestBody RuleDtos.RuleUpdateRequest request) {
        return ruleService.updateRule(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @DeleteMapping("/rules/{id}")
    public Map<String, Object> deleteRule(@PathVariable long id) {
        return ruleService.deleteRule(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PostMapping("/rules/{id}/versions")
    public Map<String, Object> createVersion(@PathVariable long id, @Valid @RequestBody RuleDtos.RuleVersionCreateRequest request) {
        return ruleService.createVersion(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PostMapping("/rule-versions/{id}/publish")
    public Map<String, Object> publish(@PathVariable long id) {
        return ruleService.publishVersion(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'rule:version:publish')")
    @PostMapping("/rule-versions/{id}/simulate")
    public Map<String, Object> simulate(@PathVariable long id, @Valid @RequestBody RuleDtos.SimulateRequest request) {
        return ruleService.simulate(id, request);
    }
}
