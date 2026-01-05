package com.tencent.wxcloudrun.security;

import com.tencent.wxcloudrun.dao.SessionMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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

public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SessionMapper sessionMapper;

    public JwtAuthFilter(JwtUtil jwtUtil, SessionMapper sessionMapper) {
        this.jwtUtil = jwtUtil;
        this.sessionMapper = sessionMapper;
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
            // 1) JWT 验签 + exp 校验（失败会抛 JwtException）
            Claims claims = jwtUtil.parse(token);

            long studentId = Long.parseLong(claims.getSubject());

            // 2) 单端登录校验：对比 token_hash
            String dbHash = sessionMapper.getTokenHash(studentId);
            if (dbHash == null || !dbHash.equals(sha256Hex(token))) {
                write401(resp, "SESSION_INVALID");
                return;
            }

            // 3) 更新 last_seen_at（可选，建议加）
            sessionMapper.touch(studentId);

            // 4) 写入 SecurityContext（后续接口可拿到 studentId）
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(studentId, null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(req, resp);
        } catch (JwtException | IllegalArgumentException e) {
            write401(resp, "TOKEN_INVALID");
        }
    }

    private static void write401(HttpServletResponse resp, String code) throws IOException {
        resp.setStatus(401);
        resp.setContentType("application/json;charset=utf-8");
        resp.getWriter().write("{\"code\":\"" + code + "\"}");
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