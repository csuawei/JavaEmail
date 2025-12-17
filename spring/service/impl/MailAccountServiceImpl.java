package com.db.spring.service.impl;

import com.db.spring.entity.MailAccount;
import com.db.spring.mapper.MailAccountMapper;
import com.db.spring.service.MailAccountService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@Service
public class MailAccountServiceImpl extends ServiceImpl<MailAccountMapper, MailAccount> implements MailAccountService {

}
