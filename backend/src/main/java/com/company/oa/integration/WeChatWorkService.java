package com.company.oa.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * 企业微信集成服务
 * 统一登录、消息推送、审批同步
 */
@Service
public class WeChatWorkService {
    private static final Logger log = LoggerFactory.getLogger(WeChatWorkService.class);
    private final RestTemplate restTemplate;

    @Value("${wechat.work.corp-id:}")
    private String corpId;

    @Value("${wechat.work.agent-id:}")
    private String agentId;

    @Value("${wechat.work.secret:}")
    private String secret;

    public WeChatWorkService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 获取访问令牌
     */
    public String getAccessToken() {
        try {
            String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/gettoken?corpid=%s&corpsecret=%s", corpId, secret);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response != null && response.containsKey("access_token")) {
                return (String) response.get("access_token");
            }
        } catch (Exception e) {
            log.error("获取企业微信访问令牌失败", e);
        }
        return null;
    }

    /**
     * 发送应用消息
     */
    public boolean sendAppMessage(String userId, String title, String content, String url) {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return false;

            String apiUrl = String.format("https://qyapi.weixin.qq.com/cgi-bin/message/send?access_token=%s", accessToken);

            Map<String, Object> body = new HashMap<>();
            body.put("touser", userId);
            body.put("msgtype", "text");
            body.put("agentid", agentId);

            Map<String, Object> text = new HashMap<>();
            text.put("content", title + "\n" + content);
            body.put("text", text);

            restTemplate.postForObject(apiUrl, body, Map.class);
            return true;
        } catch (Exception e) {
            log.error("发送企业微信消息失败", e);
            return false;
        }
    }

    /**
     * 推送审批通知
     */
    public boolean pushApprovalNotification(String userId, String approvalType, String title, Long instanceId) {
        String content = String.format("您有一条新的%s待审批\n单号: %d\n请点击查看详情", approvalType, instanceId);
        return sendAppMessage(userId, "审批通知", content, "/workflow/instances/" + instanceId);
    }

    /**
     * 同步通讯录
     */
    public List<Map<String, Object>> syncContacts() {
        try {
            String accessToken = getAccessToken();
            if (accessToken == null) return Collections.emptyList();

            String url = String.format("https://qyapi.weixin.qq.com/cgi-bin/user/list?access_token=%s", accessToken);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response != null && response.containsKey("userlist")) {
                return (List<Map<String, Object>>) response.get("userlist");
            }
        } catch (Exception e) {
            log.error("同步企业微信通讯录失败", e);
        }
        return Collections.emptyList();
    }
}
