package com.company.oa.common.service;

import org.springframework.stereotype.Component;

@Component
public class DataMaskService {

    public String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    public String maskIdCard(String idCard) {
        if (idCard == null || idCard.length() < 8) return idCard;
        return idCard.substring(0, 4) + "**********" + idCard.substring(idCard.length() - 4);
    }

    public String maskEmail(String email) {
        if (email == null || !email.contains("@")) return email;
        int at = email.indexOf('@');
        if (at <= 2) return email;
        return email.substring(0, 2) + "***" + email.substring(at);
    }

    public String maskName(String name) {
        if (name == null || name.isEmpty()) return name;
        if (name.length() == 1) return "*";
        if (name.length() == 2) return name.charAt(0) + "*";
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }
}
