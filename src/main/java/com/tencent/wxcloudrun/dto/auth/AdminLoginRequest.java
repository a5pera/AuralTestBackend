package com.tencent.wxcloudrun.dto.auth;

import lombok.Data;

@Data
public class AdminLoginRequest {
    private String username;
    private String password;
}
