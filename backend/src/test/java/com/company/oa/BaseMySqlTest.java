package com.company.oa;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.MybatisSqlSessionFactoryBuilder;
import org.apache.ibatis.mapping.Environment;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.LocalCacheScope;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.transaction.jdbc.JdbcTransactionFactory;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

import javax.sql.DataSource;

@SuppressWarnings("resource")
public abstract class BaseMySqlTest {

    private static final Object INIT_LOCK = new Object();
    private static volatile boolean initialized = false;

    protected static DataSource dataSource;
    protected static JdbcTemplate jdbc;
    protected static SqlSessionFactory sqlSessionFactory;
    protected static SqlSession sqlSession;

    @BeforeAll
    static void startContainer() {
        if (initialized) {
            return;
        }
        synchronized (INIT_LOCK) {
            if (initialized) {
                return;
            }
            String url = System.getenv().getOrDefault("DB_HOST", "localhost");
            String port = System.getenv().getOrDefault("DB_PORT", "3306");
            String dbName = System.getenv().getOrDefault("DB_NAME", "oa_system");
            String user = System.getenv().getOrDefault("DB_USER", "root");
            String password = System.getenv().getOrDefault("DB_PASSWORD", "root123456");

            String jdbcUrl = "jdbc:mysql://" + url + ":" + port + "/" + dbName
                    + "?useSSL=false&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&characterEncoding=utf8";

            DriverManagerDataSource ds = new DriverManagerDataSource(jdbcUrl, user, password);
            ds.setDriverClassName("com.mysql.cj.jdbc.Driver");
            dataSource = ds;
            jdbc = new JdbcTemplate(ds);

            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .cleanDisabled(false)
                    .baselineOnMigrate(true)
                    .load();
            flyway.repair();
            flyway.migrate();

            MybatisConfiguration configuration = new MybatisConfiguration();
            configuration.setEnvironment(new Environment("test",
                    new JdbcTransactionFactory(), dataSource));
            configuration.setLocalCacheScope(LocalCacheScope.STATEMENT);
            configuration.setMapUnderscoreToCamelCase(true);
            configuration.setDefaultExecutorType(ExecutorType.SIMPLE);

            sqlSessionFactory = new MybatisSqlSessionFactoryBuilder().build(configuration);
            sqlSession = sqlSessionFactory.openSession(true);

            registerMappers(configuration, dataSource);
            initialized = true;
        }
    }

    private static void registerMappers(MybatisConfiguration configuration, DataSource dataSource) {
        configuration.addMapper(com.company.oa.org.mapper.UserMapper.class);
        configuration.addMapper(com.company.oa.org.mapper.DeptMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysConfigMapper.class);
        configuration.addMapper(com.company.oa.audit.mapper.AuditLoginLogMapper.class);
        configuration.addMapper(com.company.oa.audit.mapper.AuditOperationLogMapper.class);
        configuration.addMapper(com.company.oa.auth.mapper.AuthSqlMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermRoleMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermMenuMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermButtonMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermUserRoleMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermRoleMenuMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermRoleButtonMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermDataScopeMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermDataScopeDeptMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermFieldPermissionMapper.class);
        configuration.addMapper(com.company.oa.permission.mapper.PermTempAuthMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfProcessTemplateMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfProcessVersionMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfProcessInstanceMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfTaskMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfTaskRecordMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfCcRecordMapper.class);
        configuration.addMapper(com.company.oa.workflow.mapper.WfDelegationMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaLeaveMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaExpenseMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaExpenseItemMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaExpenseAttachmentMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaSealApplyMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaPurchaseMapper.class);
        configuration.addMapper(com.company.oa.oa.mapper.OaPurchaseItemMapper.class);
        configuration.addMapper(com.company.oa.contract.mapper.ContractInfoMapper.class);
        configuration.addMapper(com.company.oa.asset.mapper.AssetInfoMapper.class);
        configuration.addMapper(com.company.oa.asset.mapper.AssetRecordMapper.class);
        configuration.addMapper(com.company.oa.asset.mapper.AssetSupplyMapper.class);
        configuration.addMapper(com.company.oa.asset.mapper.AssetSupplyRecordMapper.class);
        configuration.addMapper(com.company.oa.notice.mapper.OaNoticeMapper.class);
        configuration.addMapper(com.company.oa.notice.mapper.OaNoticeReadMapper.class);
        configuration.addMapper(com.company.oa.message.mapper.MsgMessageMapper.class);
        configuration.addMapper(com.company.oa.file.mapper.FileInfoMapper.class);
        configuration.addMapper(com.company.oa.file.mapper.FileLibraryFolderMapper.class);
        configuration.addMapper(com.company.oa.file.mapper.FileDownloadLogMapper.class);
        configuration.addMapper(com.company.oa.meeting.mapper.MeetingRoomMapper.class);
        configuration.addMapper(com.company.oa.meeting.mapper.MeetingBookingMapper.class);
        configuration.addMapper(com.company.oa.rule.mapper.RuleDefinitionMapper.class);
        configuration.addMapper(com.company.oa.rule.mapper.RuleGroupMapper.class);
        configuration.addMapper(com.company.oa.rule.mapper.RuleVersionMapper.class);
        configuration.addMapper(com.company.oa.rule.mapper.RuleAuditLogMapper.class);
        configuration.addMapper(com.company.oa.form.mapper.FormTemplateMapper.class);
        configuration.addMapper(com.company.oa.form.mapper.FormVersionMapper.class);
        configuration.addMapper(com.company.oa.form.mapper.FormFieldRuleMapper.class);
        configuration.addMapper(com.company.oa.form.mapper.FormSnapshotMapper.class);
        configuration.addMapper(com.company.oa.ops.mapper.JobTaskLogMapper.class);
        configuration.addMapper(com.company.oa.ops.mapper.AppExceptionLogMapper.class);
        configuration.addMapper(com.company.oa.ops.mapper.BackupRecordMapper.class);
        configuration.addMapper(com.company.oa.org.mapper.PositionMapper.class);
        configuration.addMapper(com.company.oa.org.mapper.RankMapper.class);
        configuration.addMapper(com.company.oa.org.mapper.UserDeptMapper.class);
        configuration.addMapper(com.company.oa.org.mapper.ChangeLogMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysDictTypeMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysDictItemMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysNumberRuleMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysWorkCalendarMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysImportTaskMapper.class);
        configuration.addMapper(com.company.oa.system.mapper.SysExportTaskMapper.class);
        configuration.addMapper(com.company.oa.common.mapper.SysSequenceMapper.class);
        configuration.addMapper(com.company.oa.report.mapper.ReportSqlMapper.class);
    }

    protected <T> T getMapper(Class<T> mapperClass) {
        return sqlSession.getMapper(mapperClass);
    }
}
