package com.db.spring.mapper;

import com.db.spring.entity.MailMessage;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author Su
 * @since 2025-12-04
 */
@Mapper
public interface MailMessageMapper extends BaseMapper<MailMessage> {

}
