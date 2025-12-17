package com.db.spring.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


@Data
public class MailSendDTO {
    // 发件人邮箱
    private String senderEmail;
    // 收件人邮箱
    private String receiverEmail;
    // 邮件主题
    private String subject;
    // 邮件正文（支持HTML）
    private String content;
    // 附件列表
    private List<MultipartFile> attachments;
}