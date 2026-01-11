package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.dao.AdminUserMapper;
import com.tencent.wxcloudrun.dao.SessionMapper;
import com.tencent.wxcloudrun.security.JwtAuthFilter;
import com.tencent.wxcloudrun.security.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig extends WebSecurityConfigurerAdapter {

    @Autowired private JwtUtil jwtUtil;
    @Autowired private SessionMapper sessionMapper;
    @Autowired private AdminUserMapper adminUserMapper;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                // 如果你是 JWT 无状态，更推荐 STATELESS（下面给你替换写法）
                .sessionManagement().disable()
                .authorizeRequests()
                // 1) 放行公共接口
                .antMatchers(
                        "/api/auth/login", "/api/auth/bind", "/api/ping",
                        "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**",
                        "/api/auth/adminLogin"   // ✅ 确保与你 controller 一致
                ).permitAll()

                // 2) 管理员接口：必须 ADMIN（一定要放在 anyRequest 前）
                .antMatchers("/api/admin/**").hasRole("ADMIN")

                // 3) 其他接口：只要登录
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(
                        new JwtAuthFilter(jwtUtil, sessionMapper, adminUserMapper),
                        UsernamePasswordAuthenticationFilter.class
                );
    }
}
