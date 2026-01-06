package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.auth.AuthData;
import com.tencent.wxcloudrun.dto.auth.BindRequest;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.security.JwtUtil;
import com.tencent.wxcloudrun.service.AuthService;
import io.jsonwebtoken.Claims;
import org.springframework.boot.autoconfigure.neo4j.Neo4jProperties;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final StudentMapper studentMapper;

    public AuthController(AuthService authService, StudentMapper studentMapper) {
        this.authService = authService;
        this.studentMapper = studentMapper;
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

    @GetMapping("/me")
    public ApiResponse me(Authentication authentication) {
        Long studentId = (Long) authentication.getPrincipal();
        Student s = studentMapper.findByStudentNo(String.valueOf(studentId));

        return ApiResponse.ok(s);
    }
}