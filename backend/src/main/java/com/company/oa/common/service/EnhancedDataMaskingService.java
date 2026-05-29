package com.company.oa.common.service;

import com.company.oa.auth.AuthService;
import com.company.oa.auth.AuthUser;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 增强数据脱敏服务
 * 按角色动态脱敏：HR看全号，其他人看部分
 */
@Service
public class EnhancedDataMaskingService {
    private final JdbcTemplate jdbcTemplate;
    private final AuthService authService;

    // 脱敏规则配置
    private static final Map<String, List<String>> ROLE_MASK_RULES = Map.of(
        "HR", List.of("PHONE_FULL", "ID_CARD_FULL", "BANK_CARD_FULL"),
        "FINANCE", List.of("PHONE_FULL", "BANK_CARD_FULL"),
        "ADMIN", List.of("PHONE_FULL", "ID_CARD_FULL", "BANK_CARD_FULL"),
        "MANAGER", List.of("PHONE_MASKED", "ID_CARD_MASKED")
    );

    public EnhancedDataMaskingService(JdbcTemplate jdbcTemplate, AuthService authService) {
        this.jdbcTemplate = jdbcTemplate;
        this.authService = authService;
    }

    /**
     * 根据当前用户角色脱敏数据
     */
    public Map<String, Object> maskByRole(String dataType, Map<String, Object> data) {
        AuthUser user = authService.currentUser();
        Set<String> userRoles = getUserRoles(user.id());

        // 确定脱敏级别
        String maskLevel = determineMaskLevel(userRoles);

        return maskData(dataType, data, maskLevel);
    }

    /**
     * 批量脱敏
     */
    public List<Map<String, Object>> maskListByRole(String dataType, List<Map<String, Object>> dataList) {
        return dataList.stream()
            .map(data -> maskByRole(dataType, data))
            .toList();
    }

    private Set<String> getUserRoles(long userId) {
        Set<String> roles = new HashSet<>();
        List<Map<String, Object>> userRoles = jdbcTemplate.queryForList(
            "SELECT r.role_code FROM perm_user_role ur JOIN perm_role r ON ur.role_id = r.id WHERE ur.user_id = ?",
            userId
        );
        for (Map<String, Object> role : userRoles) {
            roles.add((String) role.get("role_code"));
        }
        return roles;
    }

    private String determineMaskLevel(Set<String> roles) {
        // HR/财务/管理员可以看到完整数据
        if (roles.contains("HR") || roles.contains("FINANCE") || roles.contains("ADMIN")) {
            return "FULL";
        }
        // 经理可以看到部分脱敏
        if (roles.contains("MANAGER")) {
            return "PARTIAL";
        }
        // 其他角色完全脱敏
        return "MASKED";
    }

    private Map<String, Object> maskData(String dataType, Map<String, Object> data, String maskLevel) {
        Map<String, Object> maskedData = new HashMap<>(data);

        if ("FULL".equals(maskLevel)) {
            return maskedData; // 不脱敏
        }

        switch (dataType) {
            case "USER":
                maskUserData(maskedData, maskLevel);
                break;
            case "EXPENSE":
                maskExpenseData(maskedData, maskLevel);
                break;
            case "BANK_CARD":
                maskBankCardData(maskedData, maskLevel);
                break;
        }

        return maskedData;
    }

    private void maskUserData(Map<String, Object> data, String maskLevel) {
        if ("PARTIAL".equals(maskLevel)) {
            // 部分脱敏：手机号中间4位
            if (data.containsKey("phone")) {
                String phone = (String) data.get("phone");
                data.put("phone", maskPhonePartial(phone));
            }
            // 身份证保留前3后4
            if (data.containsKey("idCard")) {
                String idCard = (String) data.get("idCard");
                data.put("idCard", maskIdCardPartial(idCard));
            }
        } else {
            // 完全脱敏
            if (data.containsKey("phone")) {
                data.put("phone", "138****5678");
            }
            if (data.containsKey("idCard")) {
                data.put("idCard", "110***********1234");
            }
        }
    }

    private void maskExpenseData(Map<String, Object> data, String maskLevel) {
        if ("MASKED".equals(maskLevel)) {
            // 非财务人员不看具体金额
            if (data.containsKey("totalAmount")) {
                data.put("totalAmount", "***");
            }
        }
    }

    private void maskBankCardData(Map<String, Object> data, String maskLevel) {
        if ("PARTIAL".equals(maskLevel)) {
            if (data.containsKey("bankCard")) {
                String bankCard = (String) data.get("bankCard");
                data.put("bankCard", maskBankCardPartial(bankCard));
            }
        } else {
            if (data.containsKey("bankCard")) {
                data.put("bankCard", "**** **** **** 1234");
            }
        }
    }

    private String maskPhonePartial(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String maskIdCardPartial(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 3) + "*".repeat(idCard.length() - 7) + idCard.substring(idCard.length() - 4);
    }

    private String maskBankCardPartial(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) return bankCard;
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }
}
