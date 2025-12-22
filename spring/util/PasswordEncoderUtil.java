package com.db.spring.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordEncoderUtil {
    // 初始化BCrypt加密器（成本因子12，越高越安全但加密越慢）
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    // 加密密码（明文→密文）
    public static String encrypt(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    // 验证密码（明文+密文→是否匹配）
    public static boolean matches(String rawPassword, String encodedPassword) {
        return encoder.matches(rawPassword, encodedPassword);
    }
}
