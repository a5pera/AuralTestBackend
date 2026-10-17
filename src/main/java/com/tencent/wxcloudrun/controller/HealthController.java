package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 提供不依赖数据库的服务存活检查接口。
 */
@RestController
public class HealthController {

    @GetMapping("/api/ping")
    public ApiResponse ping() {
        return ApiResponse.ok("pong");
    }
}
