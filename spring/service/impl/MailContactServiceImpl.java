package com.db.spring.service.impl;

import com.db.spring.entity.MailContact;
import com.db.spring.mapper.MailContactMapper;
import com.db.spring.service.MailContactService;
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
public class MailContactServiceImpl extends ServiceImpl<MailContactMapper, MailContact> implements MailContactService {

}
