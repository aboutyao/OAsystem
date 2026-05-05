package com.company.oa;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DatabaseMigrationTest extends BaseMySqlTest {

    @Test
    void coreMigrationsCreateFoundationTablesAndSeedAdmin() {
        // Clean up data left by other tests (shared database, parallel execution)
        jdbc.update("DELETE FROM wf_cc_record");
        jdbc.update("DELETE FROM wf_task_record");
        jdbc.update("DELETE FROM wf_task");
        jdbc.update("DELETE FROM wf_process_instance");
        jdbc.update("DELETE FROM wf_delegation");
        jdbc.update("DELETE FROM oa_leave");
        jdbc.update("DELETE FROM oa_expense_item");
        jdbc.update("DELETE FROM oa_expense");
        jdbc.update("DELETE FROM oa_seal_apply");
        jdbc.update("DELETE FROM oa_purchase_item");
        jdbc.update("DELETE FROM oa_purchase");
        jdbc.update("DELETE FROM contract_info");
        jdbc.update("DELETE FROM asset_supply_record");
        jdbc.update("DELETE FROM asset_supply");
        jdbc.update("DELETE FROM asset_record");
        jdbc.update("DELETE FROM asset_info");
        jdbc.update("DELETE FROM msg_message");
        jdbc.update("DELETE FROM audit_login_log");
        jdbc.update("DELETE FROM audit_operation_log");
        jdbc.update("DELETE FROM sys_work_calendar");
        jdbc.update("DELETE FROM org_user WHERE id != 1");
        jdbc.update("DELETE FROM org_user_dept WHERE user_id != 1");

        assertThat(count("org_user")).isEqualTo(1);
        assertThat(count("perm_role")).isEqualTo(12);
        assertThat(count("perm_menu")).isEqualTo(57);
        assertThat(count("perm_role_menu")).isGreaterThan(0);
        assertThat(count("sys_config")).isGreaterThanOrEqualTo(6);
        assertThat(count("wf_process_template")).isEqualTo(6);
        assertThat(count("rule_definition")).isGreaterThanOrEqualTo(2);
        assertThat(count("oa_leave")).isEqualTo(0);
        assertThat(count("oa_expense")).isEqualTo(0);
        assertThat(count("oa_expense_item")).isEqualTo(0);
        assertThat(count("oa_seal_apply")).isEqualTo(0);
        assertThat(count("oa_purchase")).isEqualTo(0);
        assertThat(count("oa_purchase_item")).isEqualTo(0);
        assertThat(count("contract_info")).isEqualTo(0);
        assertThat(count("oa_notice")).isEqualTo(0);
        assertThat(count("oa_notice_read")).isEqualTo(0);
        assertThat(count("meeting_room")).isEqualTo(0);
        assertThat(count("meeting_booking")).isEqualTo(0);
        assertThat(count("asset_info")).isEqualTo(0);
        assertThat(count("asset_record")).isEqualTo(0);
        assertThat(count("asset_supply")).isEqualTo(0);
        assertThat(count("asset_supply_record")).isEqualTo(0);
        assertThat(count("msg_message")).isEqualTo(0);
        assertThat(count("file_library_folder")).isEqualTo(0);
        assertThat(count("file_info")).isEqualTo(0);
        assertThat(count("file_download_log")).isEqualTo(0);
        assertThat(count("audit_login_log")).isEqualTo(0);
        assertThat(count("audit_operation_log")).isEqualTo(0);
        assertThat(count("wf_cc_record")).isEqualTo(0);
        assertThat(count("wf_delegation")).isEqualTo(0);
        assertThat(count("sys_number_rule")).isGreaterThanOrEqualTo(4);
        assertThat(count("sys_work_calendar")).isEqualTo(0);
        assertThat(count("sys_import_task")).isEqualTo(0);
        assertThat(count("sys_export_task")).isEqualTo(0);
        assertThat(count("form_template")).isGreaterThanOrEqualTo(2);
        assertThat(count("form_version")).isGreaterThanOrEqualTo(2);
        assertThat(count("form_field_rule")).isEqualTo(0);
        assertThat(count("form_snapshot")).isEqualTo(0);
        assertThat(count("job_task_log")).isGreaterThanOrEqualTo(2);
        assertThat(count("app_exception_log")).isEqualTo(0);
        assertThat(count("backup_record")).isGreaterThanOrEqualTo(1);
    }

    private int count(String tableName) {
        Integer result = jdbc.queryForObject("select count(*) from " + tableName, Integer.class);
        return result != null ? result : 0;
    }
}
