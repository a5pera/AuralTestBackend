package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.auth.AuthData;

import javax.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthData loginByOpenid(String openid, HttpServletRequest request);

    AuthData bindAndLogin(String openid, String studentNo, String name, String college, HttpServletRequest request);

    AuthData adminLogin(String username, String password);

    long createAdmin(String username, String rawPassword);
}