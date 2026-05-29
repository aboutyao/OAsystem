package com.company.oa.knowledge;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/knowledge-graph")
public class KnowledgeGraphController {
    private final KnowledgeGraphService knowledgeGraphService;

    public KnowledgeGraphController(KnowledgeGraphService knowledgeGraphService) {
        this.knowledgeGraphService = knowledgeGraphService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/relations/{entityType}/{entityId}")
    public Map<String, Object> getRelations(
            @PathVariable String entityType,
            @PathVariable Long entityId) {
        return knowledgeGraphService.getEntityRelations(entityType, entityId);
    }

    @PreAuthorize("hasAnyAuthority('*', 'oa:view')")
    @GetMapping("/search")
    public List<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String entityType) {
        return knowledgeGraphService.searchEntities(keyword, entityType);
    }
}
