package com.db.spring.controller;

import com.db.spring.service.MailMessageFolderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@RestController
@RequestMapping("/mail-message-folder")
public class MailMessageFolderController {
    @Autowired
    private MailMessageFolderService mailMessageFolderService;
}
