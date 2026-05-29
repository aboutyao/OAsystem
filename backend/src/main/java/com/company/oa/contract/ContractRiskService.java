package com.company.oa.contract;

import com.company.oa.message.MessageService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 合同风险预警服务
 * 自动检测：即将到期、条款异常、付款风险
 */
@Service
public class ContractRiskService {
    private final JdbcTemplate jdbcTemplate;
    private final MessageService messageService;

    public ContractRiskService(JdbcTemplate jdbcTemplate, MessageService messageService) {
        this.jdbcTemplate = jdbcTemplate;
        this.messageService = messageService;
    }

    /**
     * 检查合同风险
     */
    public List<Map<String, Object>> checkContractRisks(Long contractId) {
        List<Map<String, Object>> risks = new ArrayList<>();

        Map<String, Object> contract = jdbcTemplate.queryForMap(
            "SELECT * FROM contract_info WHERE id = ?", contractId
        );

        // 检查1: 即将到期
        checkExpirationRisk(contract, risks);

        // 检查2: 付款风险
        checkPaymentRisk(contract, risks);

        // 检查3: 金额异常
        checkAmountAnomaly(contract, risks);

        // 检查4: 条款缺失
        checkMissingClauses(contract, risks);

        return risks;
    }

    private void checkExpirationRisk(Map<String, Object> contract, List<Map<String, Object>> risks) {
        LocalDate endDate = contract.get("end_date") != null
            ? ((java.sql.Date) contract.get("end_date")).toLocalDate()
            : null;

        if (endDate == null) return;

        long daysUntilExpiry = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);

        if (daysUntilExpiry < 0) {
            risks.add(createRisk("EXPIRED", "已过期", "HIGH",
                "合同已过期 " + Math.abs(daysUntilExpiry) + " 天"));
        } else if (daysUntilExpiry <= 30) {
            risks.add(createRisk("EXPIRING_SOON", "即将到期", "HIGH",
                "合同将在 " + daysUntilExpiry + " 天后到期"));
        } else if (daysUntilExpiry <= 90) {
            risks.add(createRisk("EXPIRING_WARNING", "到期预警", "MEDIUM",
                "合同将在 " + daysUntilExpiry + " 天后到期"));
        }
    }

    private void checkPaymentRisk(Map<String, Object> contract, List<Map<String, Object>> risks) {
        BigDecimal totalAmount = (BigDecimal) contract.get("total_amount");
        BigDecimal paidAmount = (BigDecimal) contract.get("paid_amount");

        if (totalAmount != null && paidAmount != null && totalAmount.compareTo(BigDecimal.ZERO) > 0) {
            double paymentRatio = paidAmount.doubleValue() / totalAmount.doubleValue();

            if (paymentRatio > 0.9) {
                risks.add(createRisk("PAYMENT_NEAR_COMPLETE", "付款接近完成", "LOW",
                    "已付款 " + String.format("%.1f%%", paymentRatio * 100)));
            }
        }
    }

    private void checkAmountAnomaly(Map<String, Object> contract, List<Map<String, Object>> risks) {
        BigDecimal totalAmount = (BigDecimal) contract.get("total_amount");

        if (totalAmount != null && totalAmount.compareTo(new BigDecimal("1000000")) > 0) {
            risks.add(createRisk("LARGE_AMOUNT", "大额合同", "MEDIUM",
                "合同金额 " + totalAmount + " 元，建议加强审核"));
        }
    }

    private void checkMissingClauses(Map<String, Object> contract, List<Map<String, Object>> risks) {
        // 这里可以检查合同条款是否完整
        // 简化实现
    }

    /**
     * 定时检查即将到期的合同
     */
    @Scheduled(cron = "0 0 9 * * MON-FRI") // 工作日早上9点
    public void checkExpiringContracts() {
        // 查找30天内到期的合同
        List<Map<String, Object>> expiringContracts = jdbcTemplate.queryForList(
            "SELECT id, contract_name, end_date, responsible_user_id, responsible_user_name " +
            "FROM contract_info WHERE end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) " +
            "AND status = 'ACTIVE'"
        );

        for (Map<String, Object> contract : expiringContracts) {
            Long responsibleUserId = ((Number) contract.get("responsible_user_id")).longValue();
            String contractName = (String) contract.get("contract_name");
            LocalDate endDate = ((java.sql.Date) contract.get("end_date")).toLocalDate();
            long daysLeft = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), endDate);

            // 发送提醒
            messageService.send(
                responsibleUserId,
                "CONTRACT",
                "合同到期提醒",
                String.format("合同「%s」将在 %d 天后到期，请及时处理续签或终止事宜。", contractName, daysLeft),
                "CONTRACT",
                null,
                ((Number) contract.get("id")).longValue()
            );
        }
    }

    /**
     * 获取合同风险报告
     */
    public Map<String, Object> getContractRiskReport() {
        Map<String, Object> report = new HashMap<>();

        // 已过期合同
        Long expiredCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM contract_info WHERE end_date < CURDATE() AND status = 'ACTIVE'",
            Long.class
        );
        report.put("expiredCount", expiredCount);

        // 即将到期合同（30天内）
        Long expiringCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM contract_info WHERE end_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 30 DAY) AND status = 'ACTIVE'",
            Long.class
        );
        report.put("expiringCount", expiringCount);

        // 大额合同
        Long largeAmountCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM contract_info WHERE total_amount > 1000000 AND status = 'ACTIVE'",
            Long.class
        );
        report.put("largeAmountCount", largeAmountCount);

        return report;
    }

    private Map<String, Object> createRisk(String type, String typeName, String severity, String description) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("type", type);
        risk.put("typeName", typeName);
        risk.put("severity", severity);
        risk.put("description", description);
        return risk;
    }
}
