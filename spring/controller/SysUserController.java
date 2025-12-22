package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.common.ReciveOneMail;
import com.db.spring.dto.LoginDTO;
import com.db.spring.dto.MailDTO;
import com.db.spring.dto.RegisterDTO;
import com.db.spring.entity.*;
import com.db.spring.mapper.MailMessageFolderMapper;
import com.db.spring.service.MailAttachmentService;
import com.db.spring.service.MailMessageFolderService;
import com.db.spring.service.SysUserService;
import com.db.spring.util.PasswordEncoderUtil;
import com.db.spring.util.ResultUtil;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.pop3.POP3Folder;
import org.apache.catalina.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@RestController
@RequestMapping("/sys-user")
public class SysUserController {

    @Autowired
    private SysUserService sysUserService;

    @Autowired
    private MailAccountController mailAccountController;

    @Autowired
    private MailRecipientController mailRecipientController;

    @Autowired
    private MailMessageController mailMessageController;

    @Autowired
    private MailAttachmentService mailAttachmentService;

    @GetMapping("/getAllUser")
    public Object getAllUser(){
        return sysUserService.getBaseMapper().selectList(null);
    }

    @PostMapping("/getIdByEmail")
    public Object getIdByEmail(String email){
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email",email);
        return mailAccountController.getIdByEmail(email);
    }

    @PostMapping("/register")
    public Object register(@RequestBody RegisterDTO registerDTO){

        SysUser sysUser = new SysUser();
        sysUser.setUsername(registerDTO.getUsername());
        String encryptedPassword = PasswordEncoderUtil.encrypt(registerDTO.getPassword());
        sysUser.setPassword(encryptedPassword); // 存储密文
        sysUser.setNickname(registerDTO.getNickname());

        int i = sysUserService.getBaseMapper().insert(sysUser);
        if(i==1){
            return ResultUtil.success(sysUser,"注册成功");
        }else{
            return ResultUtil.fail("501","注册失败");
        }
    }

    @PostMapping("/login")
    public Object login(@RequestBody LoginDTO loginDTO){

        String username = loginDTO.getUsername();
        String rawPassword = loginDTO.getPassword(); // 前端传入的明文密码

        // 1. 根据用户名查询用户（仅查一条）
        QueryWrapper<SysUser> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("username", username);
        List<SysUser> list = sysUserService.getBaseMapper().selectList(queryWrapper);

        if (list.isEmpty()) {
            return ResultUtil.fail("502", "用户不存在");
        }
        SysUser user = list.get(0);

        // 2. 验证密码（明文与密文匹配）
        if (PasswordEncoderUtil.matches(rawPassword, user.getPassword())) {
            return ResultUtil.success(user, "登录成功");
        } else {
            return ResultUtil.fail("502", "密码错误");
        }
    }

    @PostMapping("/pullmail")
    public Object pullmail(@RequestBody MailDTO mailDTO) throws Exception {

        List<String> uids = mailMessageController.getAllUid();
        String email = mailDTO.getEmail();
        Long id =  mailDTO.getId();
        MailAccount account = mailAccountController.getAcByidemail(email,id);

        //配置 POP3 连接参数
        Properties props = new Properties();
        props.setProperty("mail.store.protocol", "pop3"); // 协议类型：POP3
        props.setProperty("mail.pop3.host", account.getProtocolPop3()); // POP3服务器地址
        props.setProperty("mail.pop3.port", String.valueOf(account.getProtocolPop3Port())); // POP3端口
        props.setProperty("mail.pop3.auth", "true"); // 开启认证
        props.setProperty("mail.pop3.ssl.enable", "true"); // 开启 SSL 加密
        props.setProperty("mail.pop3.ssl.trust", account.getProtocolPop3()); // 信任服务器（避免证书校验失败）
        props.setProperty("mail.debug", "false"); // 关闭调试日志（开发时可设为true）

        // 关键配置：关闭POP3默认数量限制（解决服务器端限制）
        props.setProperty("mail.pop3.fetchsize", "0"); // 0=无限制（核心）
        props.setProperty("mail.pop3.batchsize", "100"); // 批量拉取（单次请求100封）
        props.setProperty("mail.pop3.connectiontimeout", "60000"); // 连接超时60秒
        props.setProperty("mail.pop3.timeout", "120000"); // 读取超时120秒（拉取大量邮件）
        props.setProperty("mail.pop3.partialfetch", "false"); // 禁用部分拉取（避免只拉邮件头）
        props.setProperty("mail.pop3.recent", "true"); // 关键：拉取所有“未被其他客户端收取”的邮件（解决重复拉取限制）


        //连接第三方邮箱
        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 认证：用户名=邮箱地址，密码=授权码（从数据库 mail_account.auth_code 取）
                return new PasswordAuthentication(account.getEmail(), account.getAuthCode());
            }
        });

        Store store = session.getStore("pop3");
        store.connect(account.getProtocolPop3(), account.getEmail(), account.getAuthCode());

        // 打开收件箱（POP3 协议默认收件箱为 "INBOX"）
        Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE); // 读写模式（可标记邮件为已读）
        POP3Folder pop3Folder = (POP3Folder) inbox;
        Message[] messages = inbox.getMessages();
        System.out.println("===== 服务器返回的总邮件数：" + messages.length);
        System.out.println(inbox.getMessageCount());

        List<MailMessage> mailMessages = new ArrayList<>();
        for (Message message : messages) {

            String uid = pop3Folder.getUID(message);
            if(uids.contains(uid)){
                continue;
            }

            MimeMessage mimeMessage = (MimeMessage) message;

            ReciveOneMail reciveOneMail = new ReciveOneMail(mimeMessage);
            //插入数据库表mailMessage
            MailMessage mailMessage = new MailMessage();
            //插入数据库表mailRecipient
            MailRecipient mailRecipient = new MailRecipient();
            //插入数据库表mailAttachment
            MailAttachment mailAttachment = new MailAttachment();

            Address[] froms = message.getFrom();
            String from = reciveOneMail.getFrom();//发件人邮箱

            mailMessage.setMailUid(uid);
            mailMessage.setSenderAccountEmail(from);//发件人邮箱

            mailMessage.setSenderEmail(email);

            mailMessage.setSubject(message.getSubject());//邮件主题

            reciveOneMail.getMailContent(mimeMessage);
            String stringBuffer = reciveOneMail.getBodyText();
            mailMessage.setContent(stringBuffer);//邮件内容

            Date sendDate = message.getSentDate();
            if(sendDate==null){
                continue;
            }
            LocalDateTime time = LocalDateTime.ofInstant(sendDate.toInstant(), ZoneId.systemDefault());
            mailMessage.setSendTime(time);//发送时间
            mailMessage.setStatus((byte) 2);//状态

            mailMessageController.insertMail(mailMessage);//插入表格



            mailRecipient.setMessageId(mailMessage.getMessageId());//邮件id
            mailRecipient.setRecipientAccountId(mailAccountController.getIdByEmail(email));
            mailRecipient.setRecipientEmail(email);
            mailRecipient.setRecipientType((byte) 1);//收件人类型
            mailRecipientController.insert(mailRecipient);//插入


            if(reciveOneMail.isContainAttach(mimeMessage)){
                reciveOneMail.saveAttachMent(mimeMessage);
                List<String> fileNames= reciveOneMail.getFileNames();
                for(String fileName : fileNames){
                        mailAttachment.setMessageId(mailMessage.getMessageId());
                        mailAttachment.setFileName(fileName);
                        mailAttachment.setFilePath("C:\\Attachments\\" + fileName);
                        mailAttachment.setFileType(ReciveOneMail.getFileSuffix(fileName));
                        System.out.println("文件名：" + mailAttachment.getFileName() + "文件名");
                        mailAttachmentService.getBaseMapper().insert(mailAttachment);
                }
            }


        }
        inbox.close();
        store.close();
        return ResultUtil.success(mailMessages);
    }

    @PostMapping("/getSendMessage")
    public Object getSendMessage(@RequestBody MailDTO mailDTO) throws Exception {
        List<String> uids = mailMessageController.getAllUid();
        String email = mailDTO.getEmail();
        Long id = mailDTO.getId();
        MailAccount account = mailAccountController.getAcByidemail(email, id);

        Properties props = new Properties();
        // 基础协议配置
        props.setProperty("mail.store.protocol", "imap"); // 协议类型：IMAP
        props.setProperty("mail.imap.host", account.getProtocolImap()); // IMAP服务器地址（如imap.qq.com）
        props.setProperty("mail.imap.port", String.valueOf(account.getProtocolImapPort())); // IMAP端口（默认993，SSL加密）
        props.setProperty("mail.imap.auth", "true"); // 开启认证（必须）
        props.setProperty("mail.imap.ssl.enable", "true"); // 开启SSL加密（生产环境必须，避免明文传输）
        props.setProperty("mail.imap.ssl.trust", account.getProtocolImap()); // 信任服务器（避免SSL证书校验失败）
        props.setProperty("mail.debug", "false"); // 关闭调试日志（开发时可设为true，查看IMAP交互日志）

        // 性能与超时配置（解决大量邮件拉取的限制）
        props.setProperty("mail.imap.fetchsize", "0"); // 0=无限制（单次拉取邮件的大小限制，0为不限制）
        props.setProperty("mail.imap.batchsize", "100"); // 批量拉取（单次请求100封邮件，减轻服务器压力）
        props.setProperty("mail.imap.connectiontimeout", "60000"); // 连接超时：60秒
        props.setProperty("mail.imap.timeout", "120000"); // 读取超时：120秒（拉取大量邮件时需延长）
        props.setProperty("mail.imap.partialfetch", "false"); // 禁用部分拉取（避免只拉取邮件头，不拉取正文/附件）
        props.setProperty("mail.imap.expunge", "false"); // 关闭自动清理（避免误删服务器邮件）

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                // 认证：用户名=邮箱地址，密码=授权码（POP3/IMAP通用，需在邮箱设置中生成）
                return new PasswordAuthentication(account.getEmail(), account.getAuthCode());
            }
        });

        Store store = null;
        Folder sentFolder = null;
        // 连接IMAP服务器（与POP3的store.connect逻辑一致，但协议为IMAP）
        store = session.getStore("imap");
        store.connect(account.getProtocolImap(), account.getEmail(), account.getAuthCode());

        // ---------------- 关键：定位已发送邮件文件夹（IMAP核心，区别于POP3的INBOX） ----------------
        // 不同邮箱的“已发送”文件夹名不同
        String sentFolderName = this.autoGetSentFolderName(store); // 动态获取适配的文件夹名
        sentFolder = store.getFolder(sentFolderName);
        IMAPFolder imapFolder = (IMAPFolder) sentFolder;

        if (sentFolder == null || !sentFolder.exists()) {
            throw new MessagingException("已发送邮件文件夹不存在：" + sentFolderName);
        }

        // 以只读模式打开（若需标记邮件为已读，可改为 Folder.READ_WRITE）
        sentFolder.open(Folder.READ_ONLY);

        // ====================== 4. 拉取并解析已发送邮件 ======================
        // 获取文件夹中的所有邮件（IMAP支持按条件筛选，如按发送时间、主题等，此处先获取全部）
        Message[] messages = sentFolder.getMessages();

        List<MailMessage> mailMessages = new ArrayList<>();
        for (Message message : messages) {

            Long imapuid = imapFolder.getUID(message);
            String uid = imapuid.toString();
            if(uids.contains(uid)){
                continue;
            }

            MimeMessage mimeMessage = (MimeMessage) message;

            ReciveOneMail reciveOneMail = new ReciveOneMail(mimeMessage);
            //插入数据库表mailMessage
            MailMessage mailMessage = new MailMessage();

            //插入数据库表mailAttachment
            MailAttachment mailAttachment = new MailAttachment();

            Address[] froms = message.getFrom();
            String from = reciveOneMail.getFrom();//发件人邮箱

            mailMessage.setMailUid(uid);
            mailMessage.setSenderAccountEmail(from);//发件人邮箱

            mailMessage.setSenderEmail(reciveOneMail.getMailAddress("TO"));

            mailMessage.setSubject(message.getSubject());//邮件主题

            reciveOneMail.getMailContent(mimeMessage);
            String stringBuffer = reciveOneMail.getBodyText();
            mailMessage.setContent(stringBuffer);//邮件内容

            Date sendDate = message.getSentDate();
            if(sendDate==null){
                continue;
            }
            LocalDateTime time = LocalDateTime.ofInstant(sendDate.toInstant(), ZoneId.systemDefault());
            mailMessage.setSendTime(time);//发送时间
            mailMessage.setStatus((byte) 2);//状态

            mailMessageController.insertMail(mailMessage);//插入表格

            if(reciveOneMail.isContainAttach(mimeMessage)){
                reciveOneMail.saveAttachMent(mimeMessage);
                List<String> fileNames= reciveOneMail.getFileNames();
                for(String fileName : fileNames){
                    mailAttachment.setMessageId(mailMessage.getMessageId());
                    mailAttachment.setFileName(fileName);
                    mailAttachment.setFilePath("C:\\Attachments\\"+fileName);
                    mailAttachment.setFileType(ReciveOneMail.getFileSuffix(fileName));
                    System.out.println("文件名："+mailAttachment.getFileName()+"文件名");
                    mailAttachmentService.getBaseMapper().insert(mailAttachment);
                }
            }


        }
        sentFolder.close();
        store.close();
        return ResultUtil.success(mailMessages);

    }


    private String autoGetSentFolderName(Store store) throws MessagingException {
        // 1. 列出所有文件夹（递归获取，包括子文件夹）
        Folder[] allFolders = store.getDefaultFolder().list("*");
        // 2. 定义已发送文件夹的关键词（中英文）
        String[] sentKeywords = {
                "已发送", "Sent", "Sent Items", "Sent Mail", "Sent Messages"
        };

        // 3. 遍历文件夹，匹配关键词
        for (Folder folder : allFolders) {
            String folderName = folder.getName();
            for (String keyword : sentKeywords) {
                if (folderName.equalsIgnoreCase(keyword) || folderName.contains(keyword)) {
                    return folderName;
                }
            }
        }

        // 4. 兜底：返回默认值
        return "Sent Items";
    }
}
