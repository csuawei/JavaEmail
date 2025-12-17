package com.db.spring.dto;

import lombok.Data;

/**
 * 登录入参DTO（与前端JSON参数一一对应）
 */
@Data
public class LoginDTO {
    private String username;

    private String password;
}
