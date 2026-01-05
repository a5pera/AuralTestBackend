package com.tencent.wxcloudrun.dao;

import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;

@Mapper
public interface SessionMapper {

    @Insert("""
    INSERT INTO student_sessions(student_id, token_hash, expires_at, last_seen_at, device_info, ip)
    VALUES(#{studentId}, #{tokenHash}, #{expiresAt}, NOW(), #{deviceInfo}, #{ip})
    ON DUPLICATE KEY UPDATE
      token_hash = VALUES(token_hash),
      issued_at = NOW(),
      expires_at = VALUES(expiresAt),
      last_seen_at = NOW(),
      device_info = VALUES(deviceInfo),
      ip = VALUES(ip)
  """)
    int upsert(@Param("studentId") long studentId,
               @Param("tokenHash") String tokenHash,
               @Param("expiresAt") LocalDateTime expiresAt,
               @Param("deviceInfo") String deviceInfo,
               @Param("ip") String ip);

    @Select("SELECT token_hash FROM student_sessions WHERE student_id=#{studentId} LIMIT 1")
    String getTokenHash(@Param("studentId") long studentId);

    @Update("UPDATE student_sessions SET last_seen_at=NOW() WHERE student_id=#{studentId}")
    int touch(@Param("studentId") long studentId);
}