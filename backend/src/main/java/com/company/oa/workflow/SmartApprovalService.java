package com.company.oa.workflow;

import com.company.oa.workflow.mapper.WfTaskMapper;
import com.company.oa.workflow.mapper.WfTaskRecordMapper;
import com.company.oa.org.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 智能审批服务
 * 基于历史数据动态选择最优审批人
 */
@Service
public class SmartApprovalService {
    private final WfTaskMapper wfTaskMapper;
    private final WfTaskRecordMapper wfTaskRecordMapper;
    private final UserMapper userMapper;

    public SmartApprovalService(WfTaskMapper wfTaskMapper, WfTaskRecordMapper wfTaskRecordMapper, UserMapper userMapper) {
        this.wfTaskMapper = wfTaskMapper;
        this.wfTaskRecordMapper = wfTaskRecordMapper;
        this.userMapper = userMapper;
    }

    /**
     * 智能推荐审批人
     * @param roleCode 角色编码
     * @param businessType 业务类型
     * @param amount 金额（可选）
     * @return 推荐的审批人列表，按推荐度排序
     */
    public List<SmartApprover> recommendApprovers(String roleCode, String businessType, Double amount) {
        // 1. 获取该角色下所有用户
        List<Long> candidateIds = userMapper.selectAllUserIdsByRoleCode(roleCode);
        if (candidateIds == null || candidateIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 计算每个候选人的综合评分
        List<SmartApprover> candidates = new ArrayList<>();
        for (Long userId : candidateIds) {
            SmartApprover approver = calculateApproverScore(userId, roleCode, businessType, amount);
            if (approver != null) {
                candidates.add(approver);
            }
        }

        // 3. 按综合评分排序
        candidates.sort(Comparator.comparingDouble(SmartApprover::getTotalScore).reversed());

        return candidates;
    }

    private SmartApprover calculateApproverScore(Long userId, String roleCode, String businessType, Double amount) {
        Map<String, Object> userSnapshot = userMapper.selectUserSnapshot(userId);
        if (userSnapshot == null) return null;

        String userName = (String) userSnapshot.getOrDefault("realName", "Unknown");

        // 响应速度评分（历史平均响应时间越短越好）
        Map<String, Object> stats = wfTaskRecordMapper.selectApprovalStats(userId);
        double avgResponseHours = 24.0;
        long totalApprovals = 0;
        if (stats != null && stats.get("avgResponseHours") != null) {
            avgResponseHours = Math.max(((Number) stats.get("avgResponseHours")).doubleValue(), 0.1);
        }
        if (stats != null && stats.get("totalApprovals") != null) {
            totalApprovals = ((Number) stats.get("totalApprovals")).longValue();
        }
        double responseScore = 1.0 / (avgResponseHours + 1);

        // 工作负荷评分（当前待办越少越好）
        Long pendingCount = wfTaskMapper.countTodoTasks(userId, "PENDING");
        long currentWorkload = pendingCount != null ? pendingCount : 0L;
        double workloadScore = 1.0 / (1.0 + currentWorkload / 5.0);

        // 经验评分（历史审批次数越多越好，但有上限）
        double experienceScore = Math.min(totalApprovals / 100.0, 1.0);

        // 综合评分
        double totalScore = responseScore * 0.4 + workloadScore * 0.4 + experienceScore * 0.2;

        return new SmartApprover(userId, userName, totalScore, avgResponseHours, currentWorkload, totalApprovals);
    }

    /**
     * 生成审批推荐理由
     */
    public String generateRecommendationReason(SmartApprover approver, int rank) {
        StringBuilder reason = new StringBuilder();
        if (rank == 1) {
            reason.append("最推荐：");
        } else {
            reason.append("第").append(rank).append("推荐：");
        }
        reason.append(approver.getUserName());
        reason.append("（平均响应").append(String.format("%.1f", approver.getAvgResponseHours())).append("小时");
        reason.append("，当前待办").append(approver.getCurrentWorkload()).append("条）");
        return reason.toString();
    }

    public static class SmartApprover {
        private final long userId;
        private final String userName;
        private final double totalScore;
        private final double avgResponseHours;
        private final long currentWorkload;
        private final long totalApprovals;

        public SmartApprover(long userId, String userName, double totalScore, double avgResponseHours, long currentWorkload, long totalApprovals) {
            this.userId = userId;
            this.userName = userName;
            this.totalScore = totalScore;
            this.avgResponseHours = avgResponseHours;
            this.currentWorkload = currentWorkload;
            this.totalApprovals = totalApprovals;
        }

        public long getUserId() { return userId; }
        public String getUserName() { return userName; }
        public double getTotalScore() { return totalScore; }
        public double getAvgResponseHours() { return avgResponseHours; }
        public long getCurrentWorkload() { return currentWorkload; }
        public long getTotalApprovals() { return totalApprovals; }
    }
}
