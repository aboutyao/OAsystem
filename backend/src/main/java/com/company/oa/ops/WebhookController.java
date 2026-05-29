package com.company.oa.ops;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    private final WebhookService webhookService;

    public WebhookController(WebhookService webhookService) {
        this.webhookService = webhookService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'ops:manage')")
    @PostMapping
    public Map<String, Object> register(@RequestBody Map<String, String> request) {
        return webhookService.registerWebhook(
            request.get("name"),
            request.get("url"),
            request.get("eventType"),
            request.get("secret")
        );
    }

    @PreAuthorize("hasAnyAuthority('*', 'ops:view')")
    @GetMapping
    public List<Map<String, Object>> list() {
        return webhookService.listWebhooks();
    }

    @PreAuthorize("hasAnyAuthority('*', 'ops:manage')")
    @DeleteMapping("/{id}")
    public void delete(@PathVariable long id) {
        webhookService.deleteWebhook(id);
    }
}
