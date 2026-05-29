package com.company.oa.workflow;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * 工作流分析服务
 * 流程瓶颈分析、效率统计
 */
@Service
public class WorkflowAnalyticsService {
    private final JdbcTemplate jdbcTemplate;

    public WorkflowAnalyticsService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 获取流程瓶颈分析
     */
    public BottleneckReport analyzeBottlenecks(LocalDateTime startTime, LocalDateTime endTime) {
        BottleneckReport report = new BottleneckReport();

        // 1. 节点耗时分析
        report.setNodeAnalysis(analyzeNodeDuration(startTime, endTime));

        // 2. 审批人效率排名
        report.setApproverRanking(analyzeApproverEfficiency(startTime, endTime));

        // 3. 识别瓶颈
        report.setBottlenecks(identifyBottlenecks(report));

        return report;
    }

    private List<NodeAnalysis> analyzeNodeDuration(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Map<String, Object>> nodeStats = jdbcTemplate.queryForList(
                "SELECT node_name as nodeName, AVG(duration_hours) as avgDurationHours, COUNT(*) as count, MAX(duration_hours) as maxDurationHours " +
                "FROM wf_task_record WHERE created_at BETWEEN ? AND ? GROUP BY node_name ORDER BY avgDurationHours DESC",
                startTime, endTime
            );

            List<NodeAnalysis> analysis = new ArrayList<>();
            for (Map<String, Object> stat : nodeStats) {
                NodeAnalysis node = new NodeAnalysis();
                node.setNodeName((String) stat.get("nodeName"));
                node.setAvgDurationHours(toDouble(stat.get("avgDurationHours")));
                node.setCount(toLong(stat.get("count")));
                node.setMaxDurationHours(toDouble(stat.get("maxDurationHours")));
                analysis.add(node);
            }
            return analysis;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<ApproverEfficiency> analyzeApproverEfficiency(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            List<Map<String, Object>> approverStats = jdbcTemplate.queryForList(
                "SELECT r.operator_id as userId, u.real_name as userName, " +
                "AVG(r.duration_hours) as avgResponseHours, COUNT(*) as totalApprovals, " +
                "(SELECT COUNT(*) FROM wf_task t WHERE t.assignee_id = r.operator_id AND t.status = 'PENDING') as pendingCount " +
                "FROM wf_task_record r LEFT JOIN org_user u ON r.operator_id = u.id " +
                "WHERE r.created_at BETWEEN ? AND ? GROUP BY r.operator_id ORDER BY avgResponseHours DESC",
                startTime, endTime
            );

            List<ApproverEfficiency> ranking = new ArrayList<>();
            for (Map<String, Object> stat : approverStats) {
                ApproverEfficiency approver = new ApproverEfficiency();
                approver.setUserId(toLong(stat.get("userId")));
                approver.setUserName((String) stat.get("userName"));
                approver.setAvgResponseHours(toDouble(stat.get("avgResponseHours")));
                approver.setTotalApprovals(toLong(stat.get("totalApprovals")));
                approver.setPendingCount(toLong(stat.get("pendingCount")));
                ranking.add(approver);
            }
            return ranking;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    private List<Bottleneck> identifyBottlenecks(BottleneckReport report) {
        List<Bottleneck> bottlenecks = new ArrayList<>();

        if (report.getNodeAnalysis() != null) {
            report.getNodeAnalysis().stream()
                .sorted(Comparator.comparingDouble(NodeAnalysis::getAvgDurationHours).reversed())
                .limit(3)
                .forEach(node -> bottlenecks.add(new Bottleneck(
                    "NODE", node.getNodeName(),
                    "平均耗时 " + String.format("%.1f", node.getAvgDurationHours()) + " 小时",
                    node.getAvgDurationHours() > 24 ? "HIGH" : "MEDIUM"
                )));
        }

        if (report.getApproverRanking() != null) {
            report.getApproverRanking().stream()
                .sorted(Comparator.comparingDouble(ApproverEfficiency::getAvgResponseHours).reversed())
                .limit(3)
                .forEach(approver -> bottlenecks.add(new Bottleneck(
                    "APPROVER", approver.getUserName(),
                    "平均响应 " + String.format("%.1f", approver.getAvgResponseHours()) + " 小时",
                    approver.getAvgResponseHours() > 48 ? "HIGH" : "MEDIUM"
                )));
        }

        return bottlenecks;
    }

    /**
     * 获取部门效率统计
     */
    public Map<String, Object> getDepartmentEfficiency(Long deptId, int days) {
        LocalDateTime startTime = LocalDateTime.now().minusDays(days);
        LocalDateTime endTime = LocalDateTime.now();

        Map<String, Object> result = new HashMap<>();
        result.put("deptId", deptId);
        result.put("period", days + " 天");

        try {
            Long submissionCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_process_instance WHERE starter_dept_id = ? AND created_at BETWEEN ? AND ?",
                Long.class, deptId, startTime, endTime
            );
            result.put("submissionCount", submissionCount != null ? submissionCount : 0);

            Long completedCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM wf_process_instance WHERE starter_dept_id = ? AND status = 'COMPLETED' AND created_at BETWEEN ? AND ?",
                Long.class, deptId, startTime, endTime
            );
            result.put("completedCount", completedCount != null ? completedCount : 0);

            double completionRate = (submissionCount != null && submissionCount > 0 && completedCount != null)
                ? (double) completedCount / submissionCount * 100 : 0;
            result.put("completionRate", String.format("%.1f%%", completionRate));
        } catch (Exception e) {
            result.put("error", e.getMessage());
        }

        return result;
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private long toLong(Object value) {
        if (value == null) return 0L;
        if (value instanceof Number) return ((Number) value).longValue();
        return 0L;
    }

    // ==================== 数据类 ====================

    public static class BottleneckReport {
        private List<NodeAnalysis> nodeAnalysis;
        private List<ApproverEfficiency> approverRanking;
        private List<Bottleneck> bottlenecks;

        public List<NodeAnalysis> getNodeAnalysis() { return nodeAnalysis; }
        public void setNodeAnalysis(List<NodeAnalysis> nodeAnalysis) { this.nodeAnalysis = nodeAnalysis; }
        public List<ApproverEfficiency> getApproverRanking() { return approverRanking; }
        public void setApproverRanking(List<ApproverEfficiency> approverRanking) { this.approverRanking = approverRanking; }
        public List<Bottleneck> getBottlenecks() { return bottlenecks; }
        public void setBottlenecks(List<Bottleneck> bottlenecks) { this.bottlenecks = bottlenecks; }
    }

    public static class NodeAnalysis {
        private String nodeName;
        private double avgDurationHours;
        private long count;
        private double maxDurationHours;

        public String getNodeName() { return nodeName; }
        public void setNodeName(String nodeName) { this.nodeName = nodeName; }
        public double getAvgDurationHours() { return avgDurationHours; }
        public void setAvgDurationHours(double avgDurationHours) { this.avgDurationHours = avgDurationHours; }
        public long getCount() { return count; }
        public void setCount(long count) { this.count = count; }
        public double getMaxDurationHours() { return maxDurationHours; }
        public void setMaxDurationHours(double maxDurationHours) { this.maxDurationHours = maxDurationHours; }
    }

    public static class ApproverEfficiency {
        private long userId;
        private String userName;
        private double avgResponseHours;
        private long totalApprovals;
        private long pendingCount;

        public long getUserId() { return userId; }
        public void setUserId(long userId) { this.userId = userId; }
        public String getUserName() { return userName; }
        public void setUserName(String userName) { this.userName = userName; }
        public double getAvgResponseHours() { return avgResponseHours; }
        public void setAvgResponseHours(double avgResponseHours) { this.avgResponseHours = avgResponseHours; }
        public long getTotalApprovals() { return totalApprovals; }
        public void setTotalApprovals(long totalApprovals) { this.totalApprovals = totalApprovals; }
        public long getPendingCount() { return pendingCount; }
        public void setPendingCount(long pendingCount) { this.pendingCount = pendingCount; }
    }

    public static class Bottleneck {
        private final String type;
        private final String name;
        private final String detail;
        private final String severity;

        public Bottleneck(String type, String name, String detail, String severity) {
            this.type = type;
            this.name = name;
            this.detail = detail;
            this.severity = severity;
        }

        public String getType() { return type; }
        public String getName() { return name; }
        public String getDetail() { return detail; }
        public String getSeverity() { return severity; }
    }
}
