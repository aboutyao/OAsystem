package com.company.oa.asset;

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

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AssetController {
    private final AssetService assetService;
    private final SupplyService supplyService;

    public AssetController(AssetService assetService, SupplyService supplyService) {
        this.assetService = assetService;
        this.supplyService = supplyService;
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/assets")
    public PageResponse<Map<String, Object>> assets(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String assetCategory,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long responsibleUserId,
            @RequestParam(required = false) String keyword
    ) {
        return assetService.list(page, size, assetCategory, status, responsibleUserId, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/assets/{id}")
    public Map<String, Object> asset(@PathVariable long id) {
        return assetService.detail(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/assets")
    public Map<String, Object> createAsset(@Valid @RequestBody AssetDtos.AssetCreateRequest request) {
        return assetService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/assets/{id}")
    public Map<String, Object> updateAsset(@PathVariable long id, @Valid @RequestBody AssetDtos.AssetUpdateRequest request) {
        return assetService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/assets/{id}/receive")
    public Map<String, Object> receive(
            @PathVariable long id,
            @RequestBody(required = false) AssetDtos.AssetActionRequest body
    ) {
        return assetService.receive(id, body != null ? body : AssetDtos.AssetActionRequest.empty());
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/assets/{id}/return")
    public Map<String, Object> returnAsset(
            @PathVariable long id,
            @RequestBody(required = false) AssetDtos.AssetReasonRequest body
    ) {
        return assetService.returnAsset(id, body != null ? body : new AssetDtos.AssetReasonRequest(null));
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/assets/{id}/repair")
    public Map<String, Object> repair(
            @PathVariable long id,
            @RequestBody(required = false) AssetDtos.AssetReasonRequest body
    ) {
        return assetService.repair(id, body != null ? body : new AssetDtos.AssetReasonRequest(null));
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/assets/{id}/scrap")
    public Map<String, Object> scrap(
            @PathVariable long id,
            @RequestBody(required = false) AssetDtos.AssetReasonRequest body
    ) {
        return assetService.scrap(id, body != null ? body : new AssetDtos.AssetReasonRequest(null));
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/assets/{id}/records")
    public List<Map<String, Object>> assetRecords(@PathVariable long id) {
        return assetService.records(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/supplies")
    public PageResponse<Map<String, Object>> supplies(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword
    ) {
        return supplyService.list(page, size, category, status, keyword);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/supplies")
    public Map<String, Object> createSupply(@Valid @RequestBody SupplyDtos.SupplyCreateRequest request) {
        return supplyService.create(request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PutMapping("/supplies/{id}")
    public Map<String, Object> updateSupply(@PathVariable long id, @Valid @RequestBody SupplyDtos.SupplyUpdateRequest request) {
        return supplyService.update(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/supplies/{id}/stock-in")
    public Map<String, Object> supplyStockIn(@PathVariable long id, @Valid @RequestBody SupplyDtos.SupplyMovementRequest request) {
        return supplyService.stockIn(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/supplies/{id}/stock-out")
    public Map<String, Object> supplyStockOut(@PathVariable long id, @Valid @RequestBody SupplyDtos.SupplyMovementRequest request) {
        return supplyService.stockOut(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/supplies/{id}/return")
    public Map<String, Object> supplyReturn(@PathVariable long id, @Valid @RequestBody SupplyDtos.SupplyMovementRequest request) {
        return supplyService.returnSupply(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:create')")
    @PostMapping("/supplies/{id}/adjust")
    public Map<String, Object> supplyAdjust(@PathVariable long id, @Valid @RequestBody SupplyDtos.SupplyAdjustRequest request) {
        return supplyService.adjust(id, request);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/supplies/{id}/records")
    public List<Map<String, Object>> supplyRecords(@PathVariable long id) {
        return supplyService.records(id);
    }

    @PreAuthorize("hasAnyAuthority('*', 'org:view')")
    @GetMapping("/supplies/records")
    public List<Map<String, Object>> supplyAllRecords(@RequestParam(required = false) Long supplyId) {
        return supplyService.records(supplyId);
    }
}
