package com.company.oa.asset;

import com.company.oa.BaseSpringTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssetServiceTest extends BaseSpringTest {

    @Autowired
    private AssetService service;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM asset_record WHERE asset_id IN (SELECT id FROM asset_info WHERE asset_no LIKE 'FA-TEST-%')");
        jdbc.update("DELETE FROM asset_info WHERE asset_no LIKE 'FA-TEST-%'");
    }

    @Test
    void createReceiveReturnRepairScrapStateMachine() {
        Map<String, Object> created = service.create(new AssetDtos.AssetCreateRequest(
                "FA-TEST-0001", "笔记本", "ELECTRONIC", "Mac", LocalDate.of(2026, 1, 1),
                new BigDecimal("12000.00"), null, 2L, "测试"
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(created.get("status")).isEqualTo("IDLE");

        Map<String, Object> received = service.receive(id, new AssetDtos.AssetActionRequest(1L, "新员工领用"));
        assertThat(received.get("status")).isEqualTo("IN_USE");

        Map<String, Object> returned = service.returnAsset(id, new AssetDtos.AssetReasonRequest("离职归还"));
        assertThat(returned.get("status")).isEqualTo("IDLE");

        Map<String, Object> repaired = service.repair(id, new AssetDtos.AssetReasonRequest("送修"));
        assertThat(repaired.get("status")).isEqualTo("REPAIRING");

        Map<String, Object> returnedFromRepair = service.returnAsset(id, new AssetDtos.AssetReasonRequest("修好归还"));
        assertThat(returnedFromRepair.get("status")).isEqualTo("IDLE");

        Map<String, Object> scrapped = service.scrap(id, new AssetDtos.AssetReasonRequest("超期报废"));
        assertThat(scrapped.get("status")).isEqualTo("SCRAPPED");

        assertThatThrownBy(() -> service.scrap(id, new AssetDtos.AssetReasonRequest("再次报废")))
                .isInstanceOf(Exception.class);

        assertThat(service.records(id)).hasSizeGreaterThanOrEqualTo(4);

        Long opCount = jdbc.queryForObject(
                "select count(*) from audit_operation_log where business_type = ? and business_id = ?",
                Long.class, "ASSET", id);
        assertThat(opCount).isGreaterThanOrEqualTo(4L);
    }

    @Test
    void duplicateAssetNoIsRejected() {
        service.create(new AssetDtos.AssetCreateRequest(
                "FA-TEST-DUP", "投影仪", "ELECTRONIC", "EP-1", LocalDate.of(2026, 1, 1),
                new BigDecimal("3000.00"), null, 2L, null
        ));
        assertThatThrownBy(() -> service.create(new AssetDtos.AssetCreateRequest(
                "FA-TEST-DUP", "重复", null, null, null, null, null, 2L, null
        ))).isInstanceOf(Exception.class);
    }

    @Test
    void receiveOnlyAllowedFromIdle() {
        Map<String, Object> created = service.create(new AssetDtos.AssetCreateRequest(
                "FA-TEST-0020", "工位电脑", "ELECTRONIC", null, null, null, 1L, 2L, null
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(created.get("status")).isEqualTo("IN_USE");

        assertThatThrownBy(() -> service.receive(id, new AssetDtos.AssetActionRequest(1L, "再次领用")))
                .isInstanceOf(Exception.class);
    }
}
