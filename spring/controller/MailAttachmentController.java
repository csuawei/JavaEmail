package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.entity.MailAttachment;
import com.db.spring.service.MailAttachmentService;
import com.db.spring.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@RestController
@RequestMapping("/mail-attachment")
public class MailAttachmentController {

    @Autowired
    private MailAttachmentService mailAttachmentService;

    @GetMapping("/download")
    public void download(@RequestParam Long messageId, HttpServletResponse response) throws IOException {
        QueryWrapper<MailAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_id",messageId);
        List<MailAttachment> mailAttachments = mailAttachmentService.getBaseMapper().selectList(queryWrapper);
        for (MailAttachment mailAttachment : mailAttachments) {
            System.out.println(mailAttachment.getFilePath());
            File attachFile = new File(mailAttachment.getFilePath());
            // 设置下载响应头（核心：触发浏览器下载，处理中文文件名乱码）
            // 设置文件MIME类型（使用表中file_type字段）
            response.setContentType(mailAttachment.getFileType());
            // 中文文件名编码（避免乱码）
            String encodeFileName = URLEncoder.encode(mailAttachment.getFileName(), StandardCharsets.UTF_8.name());
            // 强制浏览器下载（而非预览）
            response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName + "\"");
            // 禁用缓存
            response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
            response.setHeader("Pragma", "no-cache");
            response.setHeader("Expires", "0");

            // 5. 读取文件流并写入响应（自动关闭流，避免资源泄漏）
            try (FileInputStream fis = new FileInputStream(attachFile);
                 OutputStream os = response.getOutputStream()) {
                FileCopyUtils.copy(fis, os); // Spring工具类简化流拷贝
                os.flush(); // 刷新输出流
            }
        }
    }

    @GetMapping("/list")
    public Object getAttachmentList(@RequestParam Long messageId) {
        if (messageId == null || messageId <= 0) {
            return ResultUtil.fail("800", "邮件ID不能为空且必须为正整数");
        }
        // 查询该邮件的所有附件
        QueryWrapper<MailAttachment> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("message_id", messageId);
        List<MailAttachment> attachments = mailAttachmentService.list(queryWrapper);
        return ResultUtil.success(attachments);
    }

    @GetMapping("/download-by-id")
    public void downloadByPath(@RequestParam Long id, HttpServletResponse response) throws IOException {
        MailAttachment mailAttachment = mailAttachmentService.getBaseMapper().selectById(id);
        File targetFile = new File(mailAttachment.getFilePath());
        // 设置下载响应头（核心：触发浏览器下载，处理中文文件名乱码）
        // 设置文件MIME类型（使用表中file_type字段）
        response.setContentType(mailAttachment.getFileType());
        // 中文文件名编码（避免乱码）
        String encodeFileName = URLEncoder.encode(mailAttachment.getFileName(), StandardCharsets.UTF_8.name());
        // 强制浏览器下载（而非预览）
        response.setHeader("Content-Disposition", "attachment; filename=\"" + encodeFileName + "\"");
        // 禁用缓存
        response.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");

        // 5. 读取文件流并写入响应（自动关闭流，避免资源泄漏）
        try (FileInputStream fis = new FileInputStream(targetFile);
             OutputStream os = response.getOutputStream()) {
            FileCopyUtils.copy(fis, os); // Spring工具类简化流拷贝
            os.flush(); // 刷新输出流
        }

    }

}
