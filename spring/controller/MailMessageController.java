package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.dto.DraftDTO;
import com.db.spring.dto.MailSendDTO;
import com.db.spring.entity.MailAccount;
import com.db.spring.entity.MailMessage;
import com.db.spring.entity.MailRecipient;
import com.db.spring.service.MailAccountService;
import com.db.spring.service.MailMessageService;
import com.db.spring.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.mail.*;
import javax.mail.internet.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * <p>
 *  前端控制器
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@RestController
@RequestMapping("/mail-message")
public class MailMessageController {

    @Autowired
    private MailMessageService mailMessageService;

    @Autowired
    private MailAccountService mailAccountService;


    @GetMapping
    public MailMessage getMailById(@RequestParam("id") Long id){
        return mailMessageService.getBaseMapper().selectById(id);
    }

    @GetMapping("/getMailById")
    public Object getMailById0(@RequestParam("id") Long id){

        MailMessage mailMessage = mailMessageService.getBaseMapper().selectById(id);
        if(mailMessage == null){
            return ResultUtil.fail("801","返回未查询到邮件");
        }
        return ResultUtil.success(mailMessage);
    }

    @GetMapping("/getMailBycontent")
    public List<MailMessage> getMailBycontent(@RequestParam("content") String content){
        QueryWrapper<MailMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("content",content);
        return mailMessageService.getBaseMapper().selectList(queryWrapper);
    }

    @PostMapping("/insertMail")
    public int insertMail(@RequestBody MailMessage mailMessage){
        return mailMessageService.getBaseMapper().insert(mailMessage);
    }

    @GetMapping("/getAllUid")
    public List<String> getAllUid(){
        List<String> res =  new ArrayList<>();
        List<MailMessage> mailMessages = mailMessageService.getBaseMapper().selectList(null);
        for(MailMessage mailMessage : mailMessages){
            res.add(mailMessage.getMailUid());
        }
        return res;
    }

    @PostMapping("/markAsRead")
    public Object markAsRead(@RequestBody Long id) {
        if (id == null) {
            return ResultUtil.fail("802", "邮件ID不能为空");
        }
        MailMessage mailMessage = mailMessageService.getBaseMapper().selectById(id);
        if (mailMessage == null) {
            return ResultUtil.fail("801", "未查询到邮件");
        }
        // 更新阅读状态为1（已读）
        mailMessage.setReadStatus((byte) 1);
        mailMessage.setUpdateTime(LocalDateTime.now());
        mailMessageService.getBaseMapper().updateById(mailMessage);
        return ResultUtil.success("标记已读成功");
    }


    //传入email,得到email发送的所有邮件
    @GetMapping("/getMailByEmail")
    public Object getMailByEmail(@RequestParam("email") String email){
        QueryWrapper<MailMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.like("sender_account_email",email)
                .orderByDesc("send_time")
                .eq("is_deleted",0)
                .orderByAsc("status");
        List<MailMessage> mailMessages = mailMessageService.getBaseMapper().selectList(queryWrapper);
        if(mailMessages!=null && mailMessages.size()>0){
            return ResultUtil.success(mailMessages);
        }
        else {
            return ResultUtil.fail("803","未查询到邮件");
        }
    }

    @PostMapping("/send")
    public ResultUtil sendMail(MailSendDTO mailSendDTO) throws IOException {

        System.out.println(mailSendDTO);
        QueryWrapper<MailAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("email",mailSendDTO.getSenderEmail());
        MailAccount  mailAccount = mailAccountService.getBaseMapper().selectList(queryWrapper).get(0);


        // 1. 参数校验
        if (mailSendDTO.getSenderEmail() == null || mailSendDTO.getSenderEmail().isEmpty()) {
            return ResultUtil.fail("901", "发件人邮箱不能为空");
        }
        if (mailSendDTO.getReceiverEmail() == null || mailSendDTO.getReceiverEmail().isEmpty()) {
            return ResultUtil.fail("903", "收件人邮箱不能为空");
        }
        if (mailSendDTO.getSubject() == null || mailSendDTO.getSubject().isEmpty()) {
            return ResultUtil.fail("904", "邮件主题不能为空");
        }
        if (mailSendDTO.getContent() == null || mailSendDTO.getContent().isEmpty()) {
            return ResultUtil.fail("905", "邮件正文不能为空");
        }

        try {
            // 2. 动态获取发件人邮箱的SMTP配置（适配QQ/163/企业邮箱）
            Properties props = getSmtpProperties(mailSendDTO.getSenderEmail(),mailAccount);

            // 3. 创建SMTP会话
            Session session = Session.getInstance(props, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(mailSendDTO.getSenderEmail(), mailAccount.getAuthCode());
                }
            });
            session.setDebug(false); // 关闭调试日志

            // 4. 构建邮件消息
            MimeMessage message = new MimeMessage(session);
            // 发件人
            message.setFrom(new InternetAddress(mailSendDTO.getSenderEmail()));
            // 收件人（多个用逗号分隔，解析为数组）
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(mailSendDTO.getReceiverEmail()));
            // 邮件主题
            message.setSubject(mailSendDTO.getSubject());

            // 5. 构建邮件内容（正文+附件）
            Multipart multipart = new MimeMultipart();

            // 5.1 邮件正文部分（支持HTML）
            MimeBodyPart contentPart = new MimeBodyPart();
            contentPart.setContent(mailSendDTO.getContent(), "text/html;charset=UTF-8");
            multipart.addBodyPart(contentPart);

            // 5.2 处理附件
            List<MultipartFile> attachments = mailSendDTO.getAttachments();
            if (attachments != null && !attachments.isEmpty()) {
                for (MultipartFile file : attachments) {
                    if (file.isEmpty()) {
                        continue;
                    }

                    String filename = file.getOriginalFilename();
                    File attachFile = new File("C:\\Attachments\\" + filename);

                    try (FileOutputStream fos = new FileOutputStream(attachFile)) {
                        fos.write(file.getBytes());
                    }

                    // 将附件添加到邮件中
                    MimeBodyPart attachPart = new MimeBodyPart();
                    attachPart.attachFile(attachFile);
                    attachPart.setFileName(filename);
                    multipart.addBodyPart(attachPart);
                }
            }

            // 6. 设置邮件内容并发送
            message.setContent(multipart);
            message.setSentDate(new java.util.Date());
            Transport.send(message);

            // 7. 返回成功结果（若需保存邮件信息，可在此处直接调用Mapper插入，示例省略）
            return ResultUtil.success("邮件发送成功");

        } catch (MessagingException e) {
            e.printStackTrace();
            return ResultUtil.fail("906", "邮件发送失败：" + e.getMessage());
        }
    }

    /**
     * 根据发件人邮箱动态获取SMTP配置
     */
    private Properties getSmtpProperties(String senderEmail,MailAccount mailAccount) {
        Properties props = new Properties();
        props.setProperty("mail.transport.protocol", "smtp");
        props.setProperty("mail.smtp.auth", "true");
        props.setProperty("mail.smtp.ssl.enable", "true");
        props.setProperty("mail.smtp.host",mailAccount.getProtocolSmtp());
        props.setProperty("mail.smtp.port",String.valueOf(mailAccount.getProtocolSmtpPort()));
        props.setProperty("mail.smtp.ssl.trust", props.getProperty("mail.smtp.host"));
        return props;
    }

    @PostMapping("/saveDraft")
    public Object saveDraft(DraftDTO draftDTO) {
        System.out.println(draftDTO);
        MailMessage mailMessage = new MailMessage();
        mailMessage.setSenderEmail(draftDTO.getReceiverEmail());
        mailMessage.setSenderAccountEmail(draftDTO.getSenderEmail());
        mailMessage.setSubject(draftDTO.getSubject());
        if(draftDTO.getContent() != null && !draftDTO.getContent().isEmpty()){
            mailMessage.setContent(draftDTO.getContent());
        }else {
            mailMessage.setContent("暂无内容");
        }
        mailMessage.setDraftTime(LocalDateTime.now());
        mailMessage.setStatus((byte) 0);
        int i = mailMessageService.getBaseMapper().insert(mailMessage);

        if(i>0){
            return ResultUtil.success(mailMessage);
        }else {
            return ResultUtil.fail("907","保存草稿失败");
        }

    }

    @GetMapping("/getDraftsByEmail")
    public Object getDraftsByEmail(String email) {
        QueryWrapper<MailMessage> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("sender_account_email",email);
        queryWrapper.eq("status",0);
        List<MailMessage> mailMessages = mailMessageService.getBaseMapper().selectList(queryWrapper);
        if(mailMessages!=null && mailMessages.size()>0){
            return ResultUtil.success(mailMessages);
        }
        return ResultUtil.fail("908","暂无草稿");
    }

    @GetMapping("/getDraftById")
    public Object getDraftById(Long id) {
        MailMessage mailMessage = mailMessageService.getBaseMapper().selectById(id);
        if(mailMessage!=null){
            return ResultUtil.success(mailMessage);
        }
        return ResultUtil.fail("908","暂无草稿");

    }

    @GetMapping("/deleteById")
    public Object deleteById(Long id) {
        System.out.println(id);
        int i = mailMessageService.getBaseMapper().deleteById(id);
        if(i>0){
            return ResultUtil.success(null);
        }
        else return ResultUtil.fail("909","删除失败");
    }

    @GetMapping("/batchDelete")
    public Object batchDelete(@RequestParam("ids") String idsStr) {

        List<Long> ids = Arrays.stream(idsStr.split(","))
                .map(Long::parseLong)
                .collect(Collectors.toList());
        for (Long id : ids) {
            MailMessage mailMessage = mailMessageService.getBaseMapper().selectById(id);
            mailMessage.setIsDeleted((byte) 1);
            mailMessageService.getBaseMapper().updateById(mailMessage);
        }
        return ResultUtil.success(ids);
    }

    @GetMapping("/mark")
    public Object mark(@RequestParam("id") Long id,@RequestParam("status") int status) {
        MailMessage mailMessage = mailMessageService.getBaseMapper().selectById(id);
        mailMessage.setStatus((byte) status);
        mailMessageService.getBaseMapper().updateById(mailMessage);
        return ResultUtil.success(mailMessage);
    }


}
