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
@TableName("mail_contact")
public class MailContact implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "contact_id", type = IdType.AUTO)
    private Long contactId;

    @TableField("user_id")
    private Long userId;

    @TableField("contact_name")
    private String contactName;

    @TableField("contact_email")
    private String contactEmail;

    @TableField("contact_group")
    private String contactGroup;

    @TableField("phone")
    private String phone;

    @TableField("remark")
    private String remark;

    @TableField("is_star")
    private Byte isStar;

    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
