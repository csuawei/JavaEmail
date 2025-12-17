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
@TableName("mail_recipient")
public class MailRecipient implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "recipient_id", type = IdType.AUTO)
    private Long recipientId;

    @TableField("message_id")
    private Long messageId;

    @TableField("recipient_account_id")
    private Long recipientAccountId;

    @TableField("recipient_type")
    private Byte recipientType;

    @TableField("is_read")
    private Byte isRead;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("recipient_email")
    private String recipientEmail;
}
