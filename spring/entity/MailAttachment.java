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
@TableName("mail_attachment")
public class MailAttachment implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(value = "attachment_id", type = IdType.AUTO)
    private Long attachmentId;

    @TableField("message_id")
    private Long messageId;

    @TableField("file_name")
    private String fileName;

    @TableField("file_path")
    private String filePath;

    @TableField("file_type")
    private String fileType;

    @TableField("upload_time")
    private LocalDateTime uploadTime;
}
