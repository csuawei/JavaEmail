package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.common.EmailServerConfigParser;
import com.db.spring.dto.MailAccountDTO;
import com.db.spring.dto.MailDTO;
import com.db.spring.entity.MailAccount;
import com.db.spring.entity.MailMessageFolder;
import com.db.spring.entity.SysUser;
import com.db.spring.service.MailAccountService;
import com.db.spring.service.SysUserService;
import com.db.spring.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

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
@RequestMapping("/mail-account")
public class MailAccountController {
    @Autowired
    private MailAccountService mailAccountService;

    @Autowired
    private SysUserService sysUserService;


    @PostMapping("/getAcByidemail")
    public MailAccount getAcByidemail(@RequestParam String email, @RequestParam Long id){
        QueryWrapper<MailAccount> queryWrapper = new QueryWrapper<MailAccount>();
        queryWrapper.eq("email",email);
        queryWrapper.eq("user_id",id);
        return mailAccountService.getBaseMapper().selectList(queryWrapper).get(0);
    }
    @PostMapping("/getIdByEmail")
    public Long getIdByEmail(@RequestParam String email){
        return mailAccountService.getBaseMapper().selectOne(new QueryWrapper<MailAccount>().eq("email",email)).getAccountId();
    }
    @PostMapping("/getAcById")
    public Object getAcById(@RequestParam Long id){
        QueryWrapper<MailAccount> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id",id);
        List<MailAccount> list = mailAccountService.getBaseMapper().selectList(queryWrapper);
        return ResultUtil.success(list);
    }

    @PostMapping("/changeEmail")
    public Object changeEmail(@RequestBody MailDTO mailDTO){
        SysUser sysUser = sysUserService.getBaseMapper().selectById(mailDTO.getId());
        sysUser.setEmail(mailDTO.getEmail());
        int i = sysUserService.getBaseMapper().updateById(sysUser);
        if (i>0){
            return ResultUtil.success(sysUser,"切换成功");
        }
        return ResultUtil.fail("101","切换失败");
    }

    @PostMapping("/addMailAccount")
    public Object addMailAccount(@RequestBody MailAccountDTO mailAccountDTO){
        EmailServerConfigParser.EmailServerConfig config = EmailServerConfigParser.parse(mailAccountDTO.getEmail());
        MailAccount mailAccount = new MailAccount();
        mailAccount.setEmail(mailAccountDTO.getEmail());
        mailAccount.setUserId(mailAccountDTO.getId());
        mailAccount.setAuthCode(mailAccountDTO.getAuthCode());
        mailAccount.setProtocolSmtp(config.getSmtpHost());
        mailAccount.setProtocolSmtpPort(config.getSmtpPort());
        mailAccount.setProtocolPop3(config.getPop3Host());
        mailAccount.setProtocolPop3Port(config.getPop3Port());
        mailAccount.setProtocolImap(config.getImapHost());
        mailAccount.setProtocolImapPort(config.getImapPort());
        mailAccountService.getBaseMapper().insert(mailAccount);
        return ResultUtil.success(mailAccount);
    }

    @PostMapping("/deleteAccount")
    public Object deleteAccount(@RequestBody MailDTO mailDTO){
        int i = mailAccountService.getBaseMapper().deleteById(mailDTO.getId());
        if (i>0){
            return ResultUtil.success(mailDTO.getId(),"删除成功");
        }
        return ResultUtil.fail("102","删除失败");
    }
}
