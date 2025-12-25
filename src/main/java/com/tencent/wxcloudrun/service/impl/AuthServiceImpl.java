package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.config.ApiException;
import com.tencent.wxcloudrun.config.BizException;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dao.StudentSessionMapper;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dao.StudentSessionMapper;
import com.tencent.wxcloudrun.dao.StudentRosterMapper;
import com.tencent.wxcloudrun.dto.auth.AuthData;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.auth.StudentRoster;
import com.tencent.wxcloudrun.service.AuthService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final StudentMapper studentMapper;
    private final StudentSessionMapper sessionMapper;
    private final StudentRosterMapper studentRosterMapper;

    public AuthServiceImpl(StudentMapper studentMapper, StudentSessionMapper sessionMapper, StudentRosterMapper studentRosterMapper) {
        this.studentMapper = studentMapper;
        this.sessionMapper = sessionMapper;
        this.studentRosterMapper = studentRosterMapper;
    }

    @Override
    public AuthData loginByOpenid(String openid, HttpServletRequest request) {
        Student s = studentMapper.findByOpenid(openid);

        // 已存在 student：直接登录，若不存在，返回给前端让前端跳转到绑定信息界面
        if (s != null) {
            return issueSession(s.getId());
        } else {
            throw BizException.deny();
        }
    }

    @Override
    @Transactional
    public AuthData bindAndLogin(String openid, String studentNo, String name, String college, HttpServletRequest request) {
        if (isBlank(studentNo) || isBlank(name) || isBlank(college)) {
            throw new RuntimeException("BAD_REQUEST");
        }

        // 这里你要接 student_roster 三字段匹配（我先给最小可跑骨架）
        // TODO: rosterMapper.findMatch(studentNo, name, college) 不存在则 NO_MATCH

        Student byNo = studentMapper.findByStudentNo(studentNo.trim());
        if (byNo != null) {
            // 学号已绑定且不是自己 -> 不允许解绑/换绑
            if (byNo.getWechatOpenid() != null && !byNo.getWechatOpenid().equals(openid)) {
                throw new RuntimeException("STUDENT_ALREADY_BOUND");
            }
            // 绑定同一个 openid：发 token
            if (byNo.getWechatOpenid() == null) {
                studentMapper.bindOpenid(byNo.getId(), openid);
            }
            return issueSession(byNo.getId());
        }

        // 新建 student（你可以改成从 roster 导入 name/college）
        Student s = new Student();
        s.setStudentNo(studentNo.trim());
        s.setName(name.trim());
        s.setCollege(college.trim());
        s.setWechatOpenid(openid);
        s.setTheta(5.0);
        studentMapper.insert(s);

        return issueSession(s.getId());
    }

    private AuthData issueSession(Long studentId) {
        String token = studentId + "." + Base64.getUrlEncoder().withoutPadding()
                .encodeToString((UUID.randomUUID() + ":" + System.nanoTime()).getBytes(StandardCharsets.UTF_8));

        String tokenHash = sha256Hex(token);
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        sessionMapper.upsert(studentId, tokenHash, expiresAt);

        AuthData out = new AuthData();
        out.setToken(token);
        out.setExpiresAt(expiresAt);
        return out;
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

    private static boolean isBlank(String s) { return s == null || s.trim().isEmpty(); }
}
