package com.company.oa.asset;

import com.company.oa.BaseMySqlTest;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.asset.mapper.AssetSupplyMapper;
import com.company.oa.asset.mapper.AssetSupplyRecordMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SupplyServiceTest extends BaseMySqlTest {

    private SupplyService service;

    @BeforeEach
    void setUp() {
        AuthService auth = mock(AuthService.class);
        when(auth.currentUser()).thenReturn(
                new AuthUser(1L, "admin", "管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"))
        );
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        service = new SupplyService(
                getMapper(AssetSupplyMapper.class),
                getMapper(AssetSupplyRecordMapper.class),
                getMapper(SysConfigMapper.class),
                auth,
                sequenceService
        );
    }

    @Test
    void stockInOutKeepsStockNonNegative() {
        Map<String, Object> created = service.create(new SupplyDtos.SupplyCreateRequest(
                "S-0001", "A4纸", "OFFICE", "包", new BigDecimal("5"), null
        ));
        long id = ((Number) created.get("id")).longValue();
        assertThat(((BigDecimal) created.get("stockQuantity"))).isEqualByComparingTo("0");

        Map<String, Object> afterIn = service.stockIn(id, new SupplyDtos.SupplyMovementRequest(
                new BigDecimal("10"), null, "首次入库"));
        assertThat(((BigDecimal) afterIn.get("stockQuantity"))).isEqualByComparingTo("10");

        Map<String, Object> afterOut = service.stockOut(id, new SupplyDtos.SupplyMovementRequest(
                new BigDecimal("3"), 1L, "领用"));
        assertThat(((BigDecimal) afterOut.get("stockQuantity"))).isEqualByComparingTo("7");

        assertThatThrownBy(() -> service.stockOut(id, new SupplyDtos.SupplyMovementRequest(
                new BigDecimal("100"), 1L, "超额领用")))
                .isInstanceOf(Exception.class);

        BigDecimal stillStock = jdbc.queryForObject(
                "select stock_quantity from asset_supply where id = ?", BigDecimal.class, id);
        assertThat(stillStock).isEqualByComparingTo("7");
    }

    @Test
    void adjustResetsStockAndRecordsDelta() {
        Map<String, Object> created = service.create(new SupplyDtos.SupplyCreateRequest(
                "S-0002", "签字笔", null, "支", null, null
        ));
        long id = ((Number) created.get("id")).longValue();

        service.stockIn(id, new SupplyDtos.SupplyMovementRequest(new BigDecimal("20"), null, "入库"));
        service.adjust(id, new SupplyDtos.SupplyAdjustRequest(new BigDecimal("18"), "盘亏"));

        BigDecimal stock = jdbc.queryForObject(
                "select stock_quantity from asset_supply where id = ?", BigDecimal.class, id);
        assertThat(stock).isEqualByComparingTo("18");

        Long count = jdbc.queryForObject(
                "select count(*) from asset_supply_record where supply_id = ? and record_type = 'ADJUST'",
                Long.class, id);
        assertThat(count).isEqualTo(1L);
    }

    @Test
    void duplicateSupplyCodeRejected() {
        service.create(new SupplyDtos.SupplyCreateRequest(
                "S-DUP", "胶水", null, "瓶", null, null
        ));
        assertThatThrownBy(() -> service.create(new SupplyDtos.SupplyCreateRequest(
                "S-DUP", "重复", null, "瓶", null, null
        ))).isInstanceOf(Exception.class);
    }
}
