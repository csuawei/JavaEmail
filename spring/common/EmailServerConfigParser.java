package com.db.spring.common;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

public class EmailServerConfigParser {
    // 主流邮箱服务器配置映射表（与前端保持一致）
    private static final Map<String, EmailServerConfig> DOMAIN_CONFIG_MAP = new HashMap<>();

    // 静态初始化配置
    static {
        // 163邮箱
        DOMAIN_CONFIG_MAP.put("163.com", new EmailServerConfig(
                "smtp.163.com", 465,
                "pop.163.com", 995,
                "imap.163.com", 993
        ));
        // QQ邮箱
        DOMAIN_CONFIG_MAP.put("qq.com", new EmailServerConfig(
                "smtp.qq.com", 465,
                "pop.qq.com", 995,
                "imap.qq.com", 993
        ));
        // 126邮箱
        DOMAIN_CONFIG_MAP.put("126.com", new EmailServerConfig(
                "smtp.126.com", 465,
                "pop.126.com", 995,
                "imap.126.com", 993
        ));
        // Outlook邮箱
        DOMAIN_CONFIG_MAP.put("outlook.com", new EmailServerConfig(
                "smtp.office365.com", 587,
                "outlook.office365.com", 995,
                "imap.office365.com", 993
        ));
        // 阿里邮箱
        DOMAIN_CONFIG_MAP.put("aliyun.com", new EmailServerConfig(
                "smtp.aliyun.com", 465,
                "pop.aliyun.com", 995,
                "imap.aliyun.com", 993
        ));
        // 企业微信/QQ企业邮箱
        DOMAIN_CONFIG_MAP.put("wx.qq.com", new EmailServerConfig(
                "smtp.exmail.qq.com", 465,
                "pop.exmail.qq.com", 995,
                "imap.exmail.qq.com", 993
        ));
    }

    /**
     * 解析邮箱地址，返回对应的服务器配置
     * @param email 邮箱地址（如 xxx@163.com）
     * @return 服务器配置（SMTP/POP3/IMAP），解析失败则抛异常
     */
    public static EmailServerConfig parse(String email) {
        // 1. 校验邮箱格式
        if (StringUtils.isBlank(email) || !email.contains("@")) {
            throw new IllegalArgumentException("邮箱格式无效：" + email);
        }
        // 2. 提取域名（@后部分）
        String domain = email.split("@")[1].toLowerCase();
        // 3. 匹配配置
        EmailServerConfig config = DOMAIN_CONFIG_MAP.get(domain);
        if (config == null) {
            throw new IllegalArgumentException("暂不支持该域名的邮箱配置解析：" + domain);
        }
        return config;
    }

    /**
     * 邮箱服务器配置实体
     */
    @Data
    public static class EmailServerConfig {
        private String smtpHost;    // SMTP服务器地址
        private int smtpPort;       // SMTP端口
        private String pop3Host;    // POP3服务器地址
        private int pop3Port;       // POP3端口
        private String imapHost;    // IMAP服务器地址
        private int imapPort;       // IMAP端口

        // 构造器
        public EmailServerConfig(String smtpHost, int smtpPort, String pop3Host, int pop3Port, String imapHost, int imapPort) {
            this.smtpHost = smtpHost;
            this.smtpPort = smtpPort;
            this.pop3Host = pop3Host;
            this.pop3Port = pop3Port;
            this.imapHost = imapHost;
            this.imapPort = imapPort;
        }

    }
}
