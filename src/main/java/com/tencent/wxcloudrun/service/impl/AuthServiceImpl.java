package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.config.BizException;
import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.auth.AuthData;
import com.tencent.wxcloudrun.model.auth.AdminUser;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.auth.StudentRoster;
import com.tencent.wxcloudrun.security.JwtUtil;
import com.tencent.wxcloudrun.service.AuthService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;
    private final SessionMapper sessionMapper;
    private final StudentRosterMapper studentRosterMapper;
    private final JwtUtil jwtUtil;
    private final AdminUserMapper adminUserMapper;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public AuthServiceImpl(StudentMapper studentMapper, SessionMapper sessionMapper,
                           StudentRosterMapper studentRosterMapper, JwtUtil jwtUtil,
                           AdminUserMapper adminUserMapper, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.studentMapper = studentMapper;
        this.sessionMapper = sessionMapper;
        this.studentRosterMapper = studentRosterMapper;
        this.jwtUtil = jwtUtil;
        this.adminUserMapper = adminUserMapper;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
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
        StudentRoster studentRoster = studentRosterMapper.findMatch(studentNo.trim(), name.trim(), college.trim());
        if (byNo != null) {  // student表中查找到了学号，之前登陆过，已经记录在student表中
            // 学号已绑定且不是自己 -> 不允许解绑/换绑
            if (byNo.getWechatOpenid() != null && !byNo.getWechatOpenid().equals(openid)) {
                throw BizException.alreadyBound();
            }
            return issueSession(byNo.getId(), request);
        }

        if(studentRoster == null) {  // 先查找白名单，白名单中存在则可以登录，不存在直接返回“NOT_ALLOWED”
            throw BizException.notAllowed();
        }

        // 新建 student（你可以改成从 roster 导入 name/college）
        Student s = new Student();
        s.setRosterId(studentRoster.getId());
        s.setStudentNo(studentNo.trim());
        s.setName(studentRoster.getName().trim());
        s.setCollege(studentRoster.getCollege().trim());
        s.setWechatOpenid(openid);
        s.setTheta(BigDecimal.valueOf(5.0));
        studentMapper.insert(s);

        return issueSession(s.getId(), request);
    }

    @Override
    public AuthData adminLogin(String username, String password) {
        if (isBlank(username) || isBlank(password)) {
            throw new IllegalArgumentException("MISSING_PARAMS");
        }

        AdminUser admin = adminUserMapper.findByUsername(username.trim());
        if (admin == null) throw new IllegalArgumentException("BAD_CREDENTIALS");
        if (Boolean.FALSE.equals(admin.getIsActive())) throw new IllegalArgumentException("ADMIN_DISABLED");

        if (!bCryptPasswordEncoder.matches(password, admin.getPasswordHash())) {
            throw new IllegalArgumentException("BAD_CREDENTIALS");
        }

        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30); // 管理员建议短一点
        Instant expInstant = expiresAt.atZone(ZoneId.systemDefault()).toInstant();
        String jti = UUID.randomUUID().toString();

        String ph = sha256Hex(admin.getPasswordHash());
        String token = jwtUtil.issueAdminToken(admin.getId(), ph, expInstant, jti);

        AuthData out = new AuthData();
        out.setToken(token);
        out.setExpiresAt(expiresAt);
        return out;
    }

    public long createAdmin(String username, String rawPassword) {
        if (isBlank(username) || isBlank(rawPassword)) {
            throw new IllegalArgumentException("MISSING_PARAMS");
        }

        String u = username.trim();
        if (adminUserMapper.findByUsername(u) != null) {
            throw new IllegalArgumentException("USERNAME_TAKEN");
        }

        AdminUser admin = new AdminUser();
        admin.setUsername(u);
        admin.setPasswordHash(bCryptPasswordEncoder.encode(rawPassword));
        admin.setIsActive(true);

        adminUserMapper.insert(admin);
        return admin.getId();
    }

    private AuthData issueSession(Long studentId, HttpServletRequest req) {
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(30);
        Instant expInstant = expiresAt.atZone(ZoneId.systemDefault()).toInstant();

        String jti = UUID.randomUUID().toString();
        String token = jwtUtil.issueToken(studentId, "STUDENT", expInstant, jti);

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
