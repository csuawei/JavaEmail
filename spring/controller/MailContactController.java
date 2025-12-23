package com.db.spring.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.db.spring.entity.MailContact;
import com.db.spring.service.MailContactService;
import com.db.spring.service.MailMessageService;
import com.db.spring.util.ResultUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
@RequestMapping("/mail-contact")
public class MailContactController {

    @Autowired
    private MailContactService mailContactService;

    /**
     * 获取联系人列表
     * @param userId 用户ID
     * @return 联系人列表
     */
    @GetMapping("/list")
    public Object getContactList(Long userId) {
        if (userId == null || userId <= 0) {
            return ResultUtil.fail("800", "用户ID不能为空且必须为正整数");
        }
        QueryWrapper<MailContact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                .eq("is_deleted", 0)
                .orderByDesc("create_time");
        List<MailContact> contacts = mailContactService.list(queryWrapper);
        return ResultUtil.success(contacts);
    }

    /**
     * 新增联系人
     * @param contact 联系人信息
     * @return 操作结果
     */
    @PostMapping("/save")
    public Object saveContact(@RequestBody MailContact contact) {
        // 基础参数校验
        if (contact.getUserId() == null || contact.getUserId() <= 0) {
            return ResultUtil.fail("801", "用户ID不能为空");
        }
        if (contact.getContactEmail() == null || contact.getContactEmail().trim().isEmpty()) {
            return ResultUtil.fail("802", "联系人邮箱不能为空");
        }

        // 校验邮箱是否已存在
        QueryWrapper<MailContact> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", contact.getUserId())
                .eq("contact_email", contact.getContactEmail().trim())
                .eq("is_deleted", 0);
        if (mailContactService.count(queryWrapper) > 0) {
            return ResultUtil.fail("803", "该邮箱已存在于联系人列表中");
        }

        // 设置默认值
        contact.setIsDeleted((byte) 0);
        contact.setIsStar((byte) 0);
        contact.setCreateTime(LocalDateTime.now());
        contact.setUpdateTime(LocalDateTime.now());

        boolean success = mailContactService.save(contact);
        return success ? ResultUtil.success(contact, "新增联系人成功") :
                ResultUtil.fail("804", "新增联系人失败");
    }

    /**
     * 更新联系人
     * @param contact 联系人信息
     * @return 操作结果
     */
    @PostMapping("/update")
    public Object updateContact(@RequestBody MailContact contact) {
        if (contact.getContactId() == null || contact.getContactId() <= 0) {
            return ResultUtil.fail("805", "联系人ID不能为空");
        }

        // 校验联系人是否存在
        MailContact existing = mailContactService.getById(contact.getContactId());
        if (existing == null || existing.getIsDeleted() == 1) {
            return ResultUtil.fail("806", "未查询到该联系人");
        }

        // 校验邮箱唯一性（排除自身）
        if (contact.getContactEmail() != null && !contact.getContactEmail().trim().isEmpty()) {
            QueryWrapper<MailContact> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("user_id", existing.getUserId())
                    .eq("contact_email", contact.getContactEmail().trim())
                    .ne("contact_id", contact.getContactId())
                    .eq("is_deleted", 0);
            if (mailContactService.count(queryWrapper) > 0) {
                return ResultUtil.fail("803", "该邮箱已存在于联系人列表中");
            }
        }

        contact.setUpdateTime(LocalDateTime.now());
        boolean success = mailContactService.updateById(contact);
        return success ? ResultUtil.success("更新联系人成功") :
                ResultUtil.fail("807", "更新联系人失败");
    }

    /**
     * 切换星标状态
     * @param params 包含contactId和isStar的参数Map
     * @return 操作结果
     */
    @PostMapping("/toggleStar")
    public Object toggleStar(@RequestBody Map<String, Object> params) {
        Long contactId = params.get("contactId") != null ? Long.valueOf(params.get("contactId").toString()) : null;
        Byte isStar = params.get("isStar") != null ? Byte.valueOf(params.get("isStar").toString()) : null;

        if (contactId == null || contactId <= 0) {
            return ResultUtil.fail("805", "联系人ID不能为空");
        }
        if (isStar == null || (isStar != 0 && isStar != 1)) {
            return ResultUtil.fail("808", "星标状态必须为0或1");
        }

        MailContact contact = mailContactService.getById(contactId);
        if (contact == null || contact.getIsDeleted() == 1) {
            return ResultUtil.fail("806", "未查询到该联系人");
        }

        contact.setIsStar(isStar);
        contact.setUpdateTime(LocalDateTime.now());
        boolean success = mailContactService.updateById(contact);
        return success ? ResultUtil.success("星标状态更新成功") :
                ResultUtil.fail("809", "星标状态更新失败");
    }

    /**
     * 删除联系人（逻辑删除）
     * @param params 包含contactId的参数Map
     * @return 操作结果
     */
    @PostMapping("/delete")
    public Object deleteContact(@RequestBody Map<String, Object> params) {
        Long contactId = params.get("contactId") != null ? Long.valueOf(params.get("contactId").toString()) : null;

        if (contactId == null || contactId <= 0) {
            return ResultUtil.fail("805", "联系人ID不能为空");
        }

        MailContact contact = mailContactService.getById(contactId);
        if (contact == null || contact.getIsDeleted() == 1) {
            return ResultUtil.fail("806", "未查询到该联系人");
        }

        contact.setIsDeleted((byte) 1);
        contact.setUpdateTime(LocalDateTime.now());
        boolean success = mailContactService.updateById(contact);
        return success ? ResultUtil.success("删除联系人成功") :
                ResultUtil.fail("810", "删除联系人失败");
    }

    /**
     * 批量删除联系人（逻辑删除）
     * @param params 包含ids列表的参数Map
     * @return 操作结果
     */
    @PostMapping("/batchDelete")
    public Object batchDelete(@RequestBody Map<String, Object> params) {
        List<?> idList = (List<?>) params.get("ids");
        if (idList == null || idList.isEmpty()) {
            return ResultUtil.fail("811", "请选择需要删除的联系人");
        }

        // 转换ID列表
        List<Long> contactIds;
        try {
            contactIds = idList.stream()
                    .map(id -> Long.valueOf(id.toString()))
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            return ResultUtil.fail("812", "联系人ID格式错误");
        }

        // 批量更新逻辑删除状态
        MailContact updateEntity = new MailContact();
        updateEntity.setIsDeleted((byte) 1);
        updateEntity.setUpdateTime(LocalDateTime.now());

        QueryWrapper<MailContact> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("contact_id", contactIds)
                .eq("is_deleted", 0);

        boolean success = mailContactService.update(updateEntity, queryWrapper);
        return success ? ResultUtil.success("批量删除成功") :
                ResultUtil.fail("813", "批量删除失败");
    }

}
