package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.entity.MailAccount;
import com.db.spring.entity.MailMessageFolder;
import com.db.spring.service.MailAccountService;
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
}
