package com.tencent.wxcloudrun.config;

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
    @Autowired
    private SessionMapper sessionMapper;

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()
                .formLogin().disable()
                .httpBasic().disable()
                .sessionManagement().disable()
                .authorizeRequests()
                .antMatchers("/api/auth/login", "/api/auth/bind", "/api/ping").permitAll()
                .anyRequest().authenticated()
                .and()
                .addFilterBefore(
                        new JwtAuthFilter(jwtUtil, sessionMapper),
                        UsernamePasswordAuthenticationFilter.class
                );
    }
}