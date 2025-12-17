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
@TableName("mail_account")
public class MailAccount implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "account_id", type = IdType.AUTO)
    private Long accountId;

    @TableField("user_id")
    private Long userId;

    @TableField("email")
    private String email;

    @TableField("protocol_smtp")
    private String protocolSmtp;

    @TableField("protocol_smtp_port")
    private Integer protocolSmtpPort;

    @TableField("protocol_pop3")
    private String protocolPop3;

    @TableField("protocol_pop3_port")
    private Integer protocolPop3Port;

    @TableField("is_default")
    private Byte isDefault;

    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;

    @TableField("auth_code")
    private String authCode;

    @TableField("protocol_imap")
    private String protocolImap;

    @TableField("protocol_imap_port")
    private Integer protocolImapPort;
}
