package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface StudentSessionMapper {

    @Insert("""
              INSERT INTO student_sessions(student_id, token_hash, issued_at, expires_at, last_seen_at)
              VALUES(#{studentId}, #{tokenHash}, NOW(), #{expiresAt}, NOW())
              ON DUPLICATE KEY UPDATE
                token_hash = VALUES(token_hash),
                issued_at = NOW(),
                expires_at = VALUES(expiresAt),
                last_seen_at = NOW()
            """)
    int upsert(@Param("studentId") Long studentId,
               @Param("tokenHash") String tokenHash,
               @Param("expiresAt") LocalDateTime expiresAt);
}
