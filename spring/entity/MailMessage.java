package com.db.spring.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * <p>
 * 
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@Getter
@Setter
@TableName("mail_message")
public class MailMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "message_id", type = IdType.AUTO)
    private Long messageId;

    @TableField("sender_account_email")
    private String senderAccountEmail;

    @TableField("subject")
    private String subject;

    @TableField("content")
    private String content;

    @TableField("send_time")
    private LocalDateTime sendTime;

    @TableField("draft_time")
    private LocalDateTime draftTime;

    @TableField("status")
    private Byte status;

    @TableField("read_status")
    private Byte readStatus;

    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("sender_email")
    private String senderEmail;

    @TableField("mail_uid")
    private String mailUid;
}
