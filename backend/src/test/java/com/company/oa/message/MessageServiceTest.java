package com.company.oa.message;

import com.company.oa.BaseMySqlTest;
import com.company.oa.audit.AuditService;
import com.company.oa.audit.mapper.AuditLoginLogMapper;
import com.company.oa.audit.mapper.AuditOperationLogMapper;
import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import com.company.oa.common.mapper.SysSequenceMapper;
import com.company.oa.common.service.SequenceService;
import com.company.oa.message.mapper.MsgMessageMapper;
import com.company.oa.system.mapper.SysConfigMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MessageServiceTest extends BaseMySqlTest {

    private MessageService service;

    @BeforeEach
    void setUp() {
        // Clean up msg_message to prevent pollution from other tests
        jdbc.update("DELETE FROM msg_message");

        AuthService authService = mock(AuthService.class);
        when(authService.currentUser()).thenReturn(
                new AuthUser(1L, "admin", "系统管理员", 2L, "总经办",
                        List.of("SUPER_ADMIN"), List.of("*"))
        );
        SequenceService sequenceService = new SequenceService(getMapper(SysSequenceMapper.class));
        AuditService auditService = new AuditService(getMapper(AuditLoginLogMapper.class),
                getMapper(AuditOperationLogMapper.class), getMapper(SysConfigMapper.class), sequenceService);
        service = new MessageService(getMapper(MsgMessageMapper.class), getMapper(SysConfigMapper.class),
                authService, auditService, sequenceService);
    }

    private long insertMessage(long id, String readStatus, String archiveStatus) {
        Timestamp now = Timestamp.from(Instant.now());
        jdbc.update("""
                        insert into msg_message (id, receiver_id, message_type, title, content, business_type,
                            business_id, wf_instance_id, read_status, archive_status, created_at, read_at)
                        values (?,?,?,?,?,?,?,?,?,?,?,?)
                        """,
                id, 1L, "TODO", "T" + id, "C" + id, "EXPENSE", id, null,
                readStatus, archiveStatus, now, null);
        return id;
    }

    @Test
    void unreadCountAndMarkReadFlowsToRead() {
        insertMessage(1001L, "UNREAD", "NORMAL");
        insertMessage(1002L, "UNREAD", "NORMAL");
        insertMessage(1003L, "READ", "NORMAL");

        assertThat(service.unreadCount().get("count")).isEqualTo(2L);

        service.markRead(1001L);
        assertThat(service.unreadCount().get("count")).isEqualTo(1L);

        service.batchRead(new MessageDtos.BatchReadRequest(List.of(1002L, 1003L)));
        assertThat(service.unreadCount().get("count")).isEqualTo(0L);
    }

    @Test
    void archivedMessageDoesNotCountAsUnread() {
        insertMessage(2001L, "UNREAD", "ARCHIVED");
        insertMessage(2002L, "UNREAD", "NORMAL");

        assertThat(service.unreadCount().get("count")).isEqualTo(1L);

        service.archive(2002L);
        assertThat(service.unreadCount().get("count")).isEqualTo(0L);
    }
}
