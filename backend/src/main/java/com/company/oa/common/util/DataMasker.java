package com.company.oa.common.util;

/**
 * 敏感数据脱敏工具类
 * 支持手机号、身份证、银行卡、邮箱、姓名等脱敏
 */
public class DataMasker {

    /**
     * 手机号脱敏
     * 138****5678
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 身份证脱敏
     * 110***********1234
     */
    public static String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) {
            return idCard;
        }
        return idCard.substring(0, 3) + "*".repeat(idCard.length() - 7) + idCard.substring(idCard.length() - 4);
    }

    /**
     * 银行卡脱敏
     * 6222 **** **** 1234
     */
    public static String maskBankCard(String bankCard) {
        if (bankCard == null || bankCard.length() < 8) {
            return bankCard;
        }
        return bankCard.substring(0, 4) + " **** **** " + bankCard.substring(bankCard.length() - 4);
    }

    /**
     * 邮箱脱敏
     * t***@example.com
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return email;
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return email;
        }
        return email.substring(0, 1) + "***" + email.substring(atIndex);
    }

    /**
     * 姓名脱敏
     * 张*、张*明
     */
    public static String maskName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }
        if (name.length() == 1) {
            return "*";
        }
        if (name.length() == 2) {
            return name.substring(0, 1) + "*";
        }
        return name.substring(0, 1) + "*".repeat(name.length() - 2) + name.substring(name.length() - 1);
    }

    /**
     * 地址脱敏
     * 北京市****小区
     */
    public static String maskAddress(String address) {
        if (address == null || address.length() <= 6) {
            return address;
        }
        return address.substring(0, 6) + "****";
    }

    /**
     * 通用脱敏
     * 保留前3后4
     */
    public static String maskGeneral(String text) {
        if (text == null || text.length() <= 7) {
            return text;
        }
        return text.substring(0, 3) + "****" + text.substring(text.length() - 4);
    }

    /**
     * 根据字段名自动选择脱敏方式
     */
    public static String autoMask(String fieldName, String value) {
        if (value == null) {
            return null;
        }
        String lower = fieldName.toLowerCase();
        if (lower.contains("phone") || lower.contains("mobile") || lower.contains("tel")) {
            return maskPhone(value);
        }
        if (lower.contains("idcard") || lower.contains("id_number") || lower.contains("identity")) {
            return maskIdCard(value);
        }
        if (lower.contains("bank") || lower.contains("card_no") || lower.contains("account")) {
            return maskBankCard(value);
        }
        if (lower.contains("email") || lower.contains("mail")) {
            return maskEmail(value);
        }
        if (lower.contains("name") && !lower.contains("username")) {
            return maskName(value);
        }
        if (lower.contains("address") || lower.contains("addr")) {
            return maskAddress(value);
        }
        return value;
    }
}
