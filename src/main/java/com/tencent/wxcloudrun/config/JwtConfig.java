package com.tencent.wxcloudrun.config;

import com.tencent.wxcloudrun.security.JwtUtil;
import lombok.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JwtConfig {

    @Bean
    public JwtUtil jwtUtil(JwtProperties props) {
        return new JwtUtil(props.getSecret());
    }
}