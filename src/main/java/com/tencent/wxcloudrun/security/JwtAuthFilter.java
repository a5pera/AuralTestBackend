package com.tencent.wxcloudrun.security;

import com.tencent.wxcloudrun.dao.AdminUserMapper;
import com.tencent.wxcloudrun.dao.SessionMapper;
import com.tencent.wxcloudrun.model.auth.AdminUser;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionMapper sessionMapper;
    private final AdminUserMapper adminUserMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, SessionMapper sessionMapper, AdminUserMapper adminUserMapper) {
        this.jwtUtil = jwtUtil;
        this.sessionMapper = sessionMapper;
        this.adminUserMapper = adminUserMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // 放行：登录、绑定、健康检查（按你项目实际路径调整）
        return "/api/auth/login".equals(path)
                || "/api/auth/bind".equals(path)
                || "/api/ping".equals(path);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {

        String auth = req.getHeader("Authorization");
        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(req, resp);
            return;
        }

        String token = auth.substring("Bearer ".length()).trim();

        try {
            Claims claims = jwtUtil.parse(token);
            String sub = claims.getSubject(); // "ADMIN:12" 或 "123"
            String role = claims.get("role", String.class);

            if (sub != null && sub.startsWith("ADMIN:")) {
                // ---------- 管理员：不走 session ----------
                long adminId = Long.parseLong(sub.substring("ADMIN:".length()));
                AdminUser admin = adminUserMapper.findById(adminId);
                if (admin == null || Boolean.FALSE.equals(admin.getIsActive())) {
                    write401(resp, "ADMIN_DISABLED");
                    return;
                }

                String phInToken = claims.get("ph", String.class);
                String phNow = sha256Hex(admin.getPasswordHash());
                if (phInToken == null || !phNow.equals(phInToken)) {
                    write401(resp, "ADMIN_TOKEN_REVOKED"); // 密码改了/旧 token 作废
                    return;
                }

                var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
                var authentication = new UsernamePasswordAuthenticationToken("ADMIN:" + adminId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);

                chain.doFilter(req, resp);
                return;
            }

            // ---------- 学生：保持你原来的 session(token_hash) 校验 ----------
            long studentId = Long.parseLong(sub);

            String dbHash = sessionMapper.getTokenHash(studentId);
            if (dbHash == null || !dbHash.equals(sha256Hex(token))) {
                write401(resp, "SESSION_INVALID");
                return;
            }
            sessionMapper.touch(studentId);

            var authorities = List.of(new SimpleGrantedAuthority("ROLE_STUDENT"));
            var authentication = new UsernamePasswordAuthenticationToken(studentId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, resp);

        } catch (JwtException | IllegalArgumentException e) {
            write401(resp, "TOKEN_INVALID");
        }
    }

    private static void write401(HttpServletResponse resp, String code) throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json;charset=utf-8");

        String msg = code == null ? "" : code;
        msg = msg.replace("\\", "\\\\").replace("\"", "\\\""); // 简单 JSON 转义

        resp.getWriter().write("{\"code\":401,\"errorMsg\":\"" + msg + "\",\"data\":{}}");
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
}