package com.db.spring.service.impl;

import com.db.spring.entity.MailMessage;
import com.db.spring.mapper.MailMessageMapper;
import com.db.spring.service.MailMessageService;
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
public class MailMessageServiceImpl extends ServiceImpl<MailMessageMapper, MailMessage> implements MailMessageService {

}
