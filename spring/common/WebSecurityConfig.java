package com.db.spring.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {
    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
                //
                // 1. 关闭CSRF防护（POST/PUT/DELETE请求不再需要CSRF Token，开发环境推荐）
                .csrf().disable()
                // 2. 关闭HTTP Basic认证（可选，若不需要）
                .httpBasic().disable()
                // 3. 不创建会话
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                // 4. 核心：放行所有请求
                .authorizeRequests()
                .antMatchers("/**") // 匹配所有路径
                .permitAll() // 允许所有用户访问（包括匿名用户）
                .anyRequest() // 其他所有请求（实际已覆盖，可省略）
                .permitAll();
    }
}
