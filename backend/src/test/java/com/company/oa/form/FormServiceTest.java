package com.company.oa.form;

import com.company.oa.BaseMySqlTest;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.error.BusinessException;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.form.mapper.FormFieldRuleMapper;
import com.company.oa.form.mapper.FormSnapshotMapper;
import com.company.oa.form.mapper.FormTemplateMapper;
import com.company.oa.form.mapper.FormVersionMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FormServiceTest extends BaseMySqlTest {

    private FormService service;

    @BeforeEach
    void setUp() {
        AuthService auth = mock(AuthService.class);
        when(auth.currentUser()).thenReturn(
                new AuthUser(1L, "admin", "管理员", 2L, "总经办", List.of("SUPER_ADMIN"), List.of("*"))
        );
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        service = new FormService(
                getMapper(FormTemplateMapper.class),
                getMapper(FormVersionMapper.class),
                getMapper(FormFieldRuleMapper.class),
                getMapper(FormSnapshotMapper.class),
                auth,
                sequenceService
        );
    }

    @Test
    void seedTemplatesArePresent() {
        var page = service.listTemplates(1, 20);
        assertThat(page.total()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void createNewVersionAndPublishArchivesPrevious() {
        Map<String, Object> v = service.createVersion(1L,
                new FormDtos.VersionCreateRequest(
                        "[{\"fieldCode\":\"x\",\"label\":\"X\"}]",
                        null, "试用扩展"
                )
        );
        long versionId = ((Number) v.get("id")).longValue();
        Map<String, Object> published = service.publishVersion(versionId);
        assertThat(published.get("status")).isEqualTo("PUBLISHED");

        Map<String, Object> tpl = service.templateDetail(1L);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> versions = (List<Map<String, Object>>) tpl.get("versions");
        long archived = versions.stream()
                .filter(x -> "ARCHIVED".equals(String.valueOf(x.get("status"))))
                .count();
        assertThat(archived).isGreaterThanOrEqualTo(1);
    }

    @Test
    void rejectsInvalidFieldsJson() {
        assertThatThrownBy(() -> service.createVersion(1L,
                new FormDtos.VersionCreateRequest("{\"not\":\"array\"}", null, null)))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void snapshotRoundtrip() {
        Map<String, Object> saved = service.saveSnapshot(
                new FormDtos.SnapshotCreateRequest(1L, "EXPENSE", 1001L, "{\"amount\":100}")
        );
        assertThat(saved.get("id")).isNotNull();
        Map<String, Object> latest = service.latestSnapshot("EXPENSE", 1001L);
        assertThat(latest.get("dataJson").toString()).contains("amount");
    }
}