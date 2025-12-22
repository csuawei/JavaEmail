package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.conditions.query.QueryChainWrapper;
import com.db.spring.dto.MailDTO;
import com.db.spring.entity.MailMessage;
import com.db.spring.entity.MailRecipient;
import com.db.spring.service.MailMessageFolderService;
import com.db.spring.service.MailMessageService;
import com.db.spring.service.MailRecipientService;
import com.db.spring.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
@RequestMapping("/mail-recipient")
public class MailRecipientController {

    @Autowired
    private MailRecipientService mailRecipientService;

    @Autowired
    private MailMessageController mailMessageController;

    @Autowired
    private MailMessageService mailMessageService;

    @PostMapping("/getMailMessage")
    public Object getMailMessage(@RequestParam String email){

        QueryWrapper<MailRecipient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("recipentEmail",email);
        List<MailRecipient> list = mailRecipientService.getBaseMapper().selectList(queryWrapper);
        if(list.isEmpty()){
            return ResultUtil.fail("601","暂无数据");
        }
        List<Long>  ids = new ArrayList<>();
        for(int i=0;i<list.size();i++){
            ids.add(i,list.get(i).getMessageId());
        }
        List<MailMessage> res = new ArrayList<>();
        for (Long id : ids) {
            res.add(mailMessageController.getMailById(id));
        }
        return ResultUtil.success(res,"查询成功");
    }

    @PostMapping("/insert")
    public int insert(@RequestBody MailRecipient mailRecipient){
        return mailRecipientService.getBaseMapper().insert(mailRecipient);
    }

    //传入email，输出email收到的所有邮件
    @GetMapping("/getMailIdByEmail")
    public Object getMailIdByEmail(@RequestParam String email) throws Exception {

        QueryWrapper<MailRecipient> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("recipient_email",email);
        List<MailRecipient> mailRecipients = mailRecipientService.getBaseMapper().selectList(queryWrapper);
        List<Long>  ids = new ArrayList<>();
        for(MailRecipient mailRecipient : mailRecipients){
            ids.add(mailRecipient.getMessageId());
        }
        List<MailMessage> res = new ArrayList<>();
        if (!ids.isEmpty()) {
            // 关键修改：查询 MailMessage 时按 sendTime 降序排列（MyBatis-Plus 语法）
            QueryWrapper<MailMessage> mailQuery = new QueryWrapper<>();
            mailQuery.in("message_id", ids)
                    .orderByDesc("send_time").eq("is_deleted",0).orderByAsc("status");
            res = mailMessageService.getBaseMapper().selectList(mailQuery);
        }
        if(res.isEmpty()){
            return ResultUtil.fail("701","暂无数据");
        }
        else {
            return ResultUtil.success(res);
        }
    }
}
