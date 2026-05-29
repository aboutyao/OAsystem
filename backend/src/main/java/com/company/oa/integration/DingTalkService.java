package com.company.oa.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 钉钉集成服务
 * 统一登录、消息推送、审批同步
 */
@Service
public class DingTalkService {
    private static final Logger log = LoggerFactory.getLogger(DingTalkService.class);
    private final RestTemplate restTemplate;

    @Value("${dingtalk.app-key:}")
    private String appKey;

    @Value("${dingtalk.app-secret:}")
    private String appSecret;

    public DingTalkService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取访问令牌
     */
    public String getAccessToken() {
        try {
            String url = String.format("https://oapi.dingtalk.com/gettoken?appkey=%s&appsecret=%s", appKey, appSecret);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
        } catch (Exception e) {
            log.error("获取钉钉访问令牌失败", e);
        }
        return null;
    }

    /**
     * 发送工作通知
     */
    public boolean sendWorkNotification(String userId, String title, String content) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return false;

            String url = String.format("https://oapi.dingtalk.com/topapi/message/corpconversation/asyncsend_v2?access_token=%s", accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("agent_id", appKey);
            body.put("userid_list", userId);

            Map<String, Object> msg = new HashMap<>();
            msg.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", title + "\n" + content);
            msg.put("text", text);
            body.put("msg", msg);

            restTemplate.postForObject(url, body, Map.class);
            return true;
        } catch (Exception e) {
            log.error("发送钉钉工作通知失败", e);
            return false;
        }
    }

    /**
     * 推送审批通知
     */
    public boolean pushApprovalNotification(String userId, String approvalType, String title, Long instanceId) {
        String content = String.format("您有一条新的%s待审批\n单号: %d\n请点击查看详情", approvalType, instanceId);
        return sendWorkNotification(userId, "审批通知", content);
    }

    /**
     * 获取审批实例列表
     */
    public List<Map<String, Object>> getApprovalInstances(String processCode, int page, int size) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return Collections.emptyList();

            String url = String.format("https://oapi.dingtalk.com/topapi/processinstance/listids?access_token=%s", accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("process_code", processCode);
            body.put("start_time", System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L);
            body.put("end_time", System.currentTimeMillis());
            body.put("size", size);
            body.put("cursor", (page - 1) * size);

            Map<String, Object> response = restTemplate.postForObject(url, body, Map.class);
            if (response != null && response.containsKey("result")) {
                Map<String, Object> result = (Map<String, Object>) response.get("result");
                return (List<Map<String, Object>>) result.get("list");
            }
        } catch (Exception e) {
            log.error("获取钉钉审批实例失败", e);
        }
        return Collections.emptyList();
    }
}
