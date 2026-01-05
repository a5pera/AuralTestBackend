package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.config.BizException;
import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.auth.AuthData;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.security.JwtUtil;
import com.tencent.wxcloudrun.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;
    private final SessionMapper sessionMapper;
    private final StudentRosterMapper studentRosterMapper;
    private final JwtUtil jwtUtil;

    public AuthServiceImpl(StudentMapper studentMapper, SessionMapper sessionMapper,
                           StudentRosterMapper studentRosterMapper, JwtUtil jwtUtil) {
        this.studentMapper = studentMapper;
        this.sessionMapper = sessionMapper;
        this.studentRosterMapper = studentRosterMapper;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public AuthData loginByOpenid(String openid, HttpServletRequest request) {
        Student s = studentMapper.findByOpenid(openid);

        // 已存在 student：直接登录，若不存在，返回给前端让前端跳转到绑定信息界面
        if (s != null) {
            return issueSession(s.getId(), request);
        }
        throw BizException.deny();
    }

    @Override
    @Transactional
    public AuthData bindAndLogin(String openid, String studentNo, String name, String college, HttpServletRequest request) {
        if (isBlank(studentNo) || isBlank(name) || isBlank(college)) {
            throw BizException.missingParams();
        }

        Student byNo = studentMapper.findByStudentNo(studentNo.trim());
        if (byNo != null) {
            // 学号已绑定且不是自己 -> 不允许解绑/换绑
            if (byNo.getWechatOpenid() != null && !byNo.getWechatOpenid().equals(openid)) {
                throw BizException.alreadyBound();
            }
            // 绑定同一个 openid：发 token
            if (byNo.getWechatOpenid() == null) {
                studentMapper.bindOpenid(byNo.getId(), openid);
            }
            return issueSession(byNo.getId(), request);
        }

        // 新建 student（你可以改成从 roster 导入 name/college）
        Student s = new Student();
        s.setStudentNo(studentNo.trim());
        s.setName(name.trim());
        s.setCollege(college.trim());
        s.setWechatOpenid(openid);
        s.setTheta(5.0);
        studentMapper.insert(s);

        return issueSession(s.getId(), request);
    }

    private AuthData issueSession(Long studentId, HttpServletRequest req) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);
        Instant expInstant = expiresAt.atZone(ZoneId.systemDefault()).toInstant();

        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.issueToken(studentId, expInstant, jti);

        String tokenHash = sha256Hex(token);

        String deviceInfo = req.getHeader("User-Agent"); // 小程序云托管可能为空，允许为空
        String ip = extractIp(req); // 你可以先简化成 req.getRemoteAddr()

        sessionMapper.upsert(studentId, tokenHash, expiresAt, deviceInfo, ip);

        AuthData out = new AuthData();
        out.setToken(token);
        out.setExpiresAt(expiresAt);
        return out;
    }

    private static String extractIp(HttpServletRequest req) {
        // 先简单版：云托管/反代下真实 IP 可能在 X-Forwarded-For
        String xff = req.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) return xff.split(",")[0].trim();
        return req.getRemoteAddr();
    }

    private static String sha256Hex(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] dig = md.digest(s.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(dig.length * 2);
            for (byte b : dig) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
