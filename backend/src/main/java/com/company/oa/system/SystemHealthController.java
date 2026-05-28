package com.company.oa.system;

import com.company.oa.org.mapper.UserMapper;
import com.company.oa.workflow.mapper.WfProcessInstanceMapper;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.Statement;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

@RestController
@RequestMapping("/api/ops")
public class SystemHealthController {

    private static final int ONLINE_THRESHOLD_MINUTES = 30;

    private final DataSource dataSource;
    private final StringRedisTemplate redisTemplate;
    private final UserMapper userMapper;
    private final WfProcessInstanceMapper wfProcessInstanceMapper;

    public SystemHealthController(DataSource dataSource,
                                  StringRedisTemplate redisTemplate,
                                  UserMapper userMapper,
                                  WfProcessInstanceMapper wfProcessInstanceMapper) {
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.userMapper = userMapper;
        this.wfProcessInstanceMapper = wfProcessInstanceMapper;
    }

    @PreAuthorize("hasAnyAuthority('*', 'audit:view')")
    @GetMapping("/system-health")
    public Map<String, Object> systemHealth() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("service", "oa-system");
        result.put("time", OffsetDateTime.now());

        result.put("database", checkDatabase());
        result.put("redis", checkRedis());
        result.put("jvm", checkJvm());
        result.put("disk", checkDisk());
        result.put("onlineUsers", countOnlineUsers());
        result.put("activeWorkflows", countActiveWorkflows());
        result.put("uptime", getUptime());

        return result;
    }

    private Map<String, Object> checkDatabase() {
        Map<String, Object> db = new LinkedHashMap<>();
        try (Connection conn = dataSource.getConnection()) {
            db.put("status", "UP");

            // HikariCP pool stats via JMX
            try {
                Object poolMXBean = getHikariPoolMXBean();
                if (poolMXBean != null) {
                    db.put("activeConnections",
                            poolMXBean.getClass().getMethod("getActiveConnections").invoke(poolMXBean));
                    db.put("totalConnections",
                            poolMXBean.getClass().getMethod("getTotalConnections").invoke(poolMXBean));
                    db.put("idleConnections",
                            poolMXBean.getClass().getMethod("getIdleConnections").invoke(poolMXBean));
                    db.put("threadsAwaitingConnection",
                            poolMXBean.getClass().getMethod("getThreadsAwaitingConnection").invoke(poolMXBean));
                }
            } catch (Exception ignored) {
            }

            // Database metadata
            DatabaseMetaData meta = conn.getMetaData();
            db.put("databaseProduct", meta.getDatabaseProductVersion());
            db.put("driver", meta.getDriverName() + " " + meta.getDriverVersion());
        } catch (Exception e) {
            db.put("status", "DOWN");
            db.put("error", e.getMessage());
        }
        return db;
    }

    private Object getHikariPoolMXBean() {
        try {
            var mBeanServer = ManagementFactory.getPlatformMBeanServer();
            var objectName = new javax.management.ObjectName("com.zaxxer.hikari:type=Pool (*");
            var names = mBeanServer.queryNames(objectName, null);
            if (!names.isEmpty()) {
                return javax.management.JMX.newMBeanProxy(mBeanServer,
                        names.iterator().next(),
                        Class.forName("com.zaxxer.hikari.HikariPoolMXBean"));
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Map<String, Object> checkRedis() {
        Map<String, Object> redis = new LinkedHashMap<>();
        try (RedisConnection connection = redisTemplate.getConnectionFactory().getConnection()) {
            String pong = connection.ping();
            redis.put("status", "PONG".equals(pong) ? "UP" : "DOWN");

            // Redis INFO memory
            try {
                Properties info = connection.info("memory");
                redis.put("usedMemory", info.getProperty("used_memory_human", "N/A"));
                redis.put("usedMemoryPeak", info.getProperty("used_memory_peak_human", "N/A"));
                redis.put("maxMemory", info.getProperty("maxmemory_human", "0"));
                double fragRatio = Double.parseDouble(info.getProperty("mem_fragmentation_ratio", "0"));
                redis.put("fragmentationRatio", Math.round(fragRatio * 100.0) / 100.0);
            } catch (Exception ignored) {
            }

            // Redis INFO clients
            try {
                Properties clientInfo = connection.info("clients");
                redis.put("connectedClients",
                        Integer.parseInt(clientInfo.getProperty("connected_clients", "0")));
                redis.put("blockedClients",
                        Integer.parseInt(clientInfo.getProperty("blocked_clients", "0")));
            } catch (Exception ignored) {
            }

            // Redis INFO server
            try {
                Properties serverInfo = connection.info("server");
                redis.put("redisVersion", serverInfo.getProperty("redis_version", "N/A"));
                redis.put("uptimeDays", serverInfo.getProperty("uptime_in_days", "N/A"));
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            redis.put("status", "DOWN");
            redis.put("error", e.getMessage());
        }
        return redis;
    }

    private Map<String, Object> checkJvm() {
        Map<String, Object> jvm = new LinkedHashMap<>();

        MemoryMXBean memoryMXBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heap = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeap = memoryMXBean.getNonHeapMemoryUsage();

        jvm.put("heapUsedMB", bytesToMB(heap.getUsed()));
        jvm.put("heapMaxMB", bytesToMB(heap.getMax()));
        jvm.put("heapUsedPercent", heap.getMax() > 0
                ? Math.round((double) heap.getUsed() / heap.getMax() * 10000.0) / 100.0
                : 0);
        jvm.put("nonHeapUsedMB", bytesToMB(nonHeap.getUsed()));

        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        jvm.put("availableProcessors", Runtime.getRuntime().availableProcessors());

        OperatingSystemMXBean osMXBean = ManagementFactory.getOperatingSystemMXBean();
        jvm.put("systemLoadAverage",
                Math.round(osMXBean.getSystemLoadAverage() * 100.0) / 100.0);

        jvm.put("threadCount", ManagementFactory.getThreadMXBean().getThreadCount());
        jvm.put("peakThreadCount", ManagementFactory.getThreadMXBean().getPeakThreadCount());
        jvm.put("daemonThreadCount", ManagementFactory.getThreadMXBean().getDaemonThreadCount());

        return jvm;
    }

    private Map<String, Object> checkDisk() {
        Map<String, Object> disk = new LinkedHashMap<>();
        File root = new File("/");
        disk.put("totalGB", roundTo2(root.getTotalSpace() / (1024.0 * 1024.0 * 1024.0)));
        disk.put("freeGB", roundTo2(root.getFreeSpace() / (1024.0 * 1024.0 * 1024.0)));
        disk.put("usableGB", roundTo2(root.getUsableSpace() / (1024.0 * 1024.0 * 1024.0)));
        long total = root.getTotalSpace();
        long free = root.getFreeSpace();
        disk.put("usedPercent", total > 0
                ? Math.round((double) (total - free) / total * 10000.0) / 100.0
                : 0);
        return disk;
    }

    private int countOnlineUsers() {
        try {
            LocalDateTime threshold = LocalDateTime.now().minusMinutes(ONLINE_THRESHOLD_MINUTES);
            return userMapper.selectRecentLogins(threshold).size();
        } catch (Exception e) {
            return -1;
        }
    }

    private long countActiveWorkflows() {
        // Direct SQL count of currently active (APPROVING) workflow instances
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "select count(*) from wf_process_instance where status = 'APPROVING'")) {
            if (rs.next()) return rs.getLong(1);
        } catch (Exception ignored) {
        }
        return 0;
    }

    private Map<String, Object> getUptime() {
        Map<String, Object> uptime = new LinkedHashMap<>();
        RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
        long uptimeMs = runtimeMXBean.getUptime();
        long days = uptimeMs / (1000 * 60 * 60 * 24);
        long hours = (uptimeMs % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (uptimeMs % (1000 * 60 * 60)) / (1000 * 60);
        uptime.put("days", days);
        uptime.put("hours", hours);
        uptime.put("minutes", minutes);
        uptime.put("formatted", days + "d " + hours + "h " + minutes + "m");
        uptime.put("startTimeMs", runtimeMXBean.getStartTime());
        return uptime;
    }

    private static double bytesToMB(long bytes) {
        return Math.round((double) bytes / (1024.0 * 1024.0) * 100.0) / 100.0;
    }

    private static double roundTo2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
