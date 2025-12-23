package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.auth.BindRequest;
import com.tencent.wxcloudrun.service.AuthService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse login(HttpServletRequest request) {
        String openid = request.getHeader("X-WX-OPENID");
        if (openid == null || openid.isBlank()) {
            return ApiResponse.error("Missing X-WX-OPENID. 请确认使用 wx.cloud.callContainer 调用云托管");
        }
        return ApiResponse.ok(authService.loginByOpenid(openid.trim(), request));
    }

    @PostMapping("/bind")
    public ApiResponse bind(@RequestBody BindRequest req, HttpServletRequest request) {
        String openid = request.getHeader("X-WX-OPENID");
        if (openid == null || openid.isBlank()) {
            return ApiResponse.error("Missing X-WX-OPENID. 请确认使用 wx.cloud.callContainer 调用云托管");
        }
        return ApiResponse.ok(authService.bindAndLogin(
                openid.trim(),
                req.getStudentNo(),
                req.getName(),
                req.getCollege(),
                request
        ));
    }
}