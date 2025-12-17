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
@TableName("mail_folder")
public class MailFolder implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "folder_id", type = IdType.AUTO)
    private Long folderId;

    @TableField("user_id")
    private Long userId;

    @TableField("folder_name")
    private String folderName;

    @TableField("folder_type")
    private Byte folderType;

    @TableField("sort_num")
    private Integer sortNum;

    @TableField("is_deleted")
    private Byte isDeleted;

    @TableField("create_time")
    private LocalDateTime createTime;

    @TableField("update_time")
    private LocalDateTime updateTime;
}
