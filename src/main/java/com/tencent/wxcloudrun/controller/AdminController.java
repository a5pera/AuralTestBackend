package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.auth.AdminLoginRequest;
import com.tencent.wxcloudrun.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;

    public AdminController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/create")
    public ApiResponse createAdmin(@RequestBody AdminLoginRequest req) {
        try {
            return ApiResponse.ok(authService.createAdmin(req.getUsername(), req.getPassword()));
        } catch (IllegalArgumentException e) {
            // 你现在 adminLogin 里抛的是 IllegalArgumentException("BAD_CREDENTIALS" / ...)
            return ApiResponse.error(e.getMessage());
        }
    }
}
