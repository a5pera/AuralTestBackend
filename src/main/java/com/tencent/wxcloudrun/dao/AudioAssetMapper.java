package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.AudioAsset;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AudioAssetMapper {

    @Insert("""
                INSERT INTO audio_assets(local_path, mime_type, duration_ms, bytes, sha256)
                VALUES(#{localPath}, #{mimeType}, #{durationMs}, #{bytes}, #{sha256})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AudioAsset a);

    @Select("""
                SELECT id,
                       local_path AS localPath,
                       mime_type AS mimeType,
                       duration_ms AS durationMs,
                       bytes,
                       sha256,
                       created_at AS createdAt
                FROM audio_assets
                WHERE id = #{id}
                LIMIT 1
            """)
    AudioAsset findById(@Param("id") long id);

    @Select("""
                SELECT id,
                       local_path AS localPath,
                       mime_type AS mimeType,
                       duration_ms AS durationMs,
                       bytes,
                       sha256,
                       created_at AS createdAt
                FROM audio_assets
                WHERE sha256 = #{sha256}
                ORDER BY id DESC
                LIMIT 1
            """)
    AudioAsset findBySha256(@Param("sha256") String sha256);

    @Select("""
                SELECT id,
                       local_path AS localPath,
                       mime_type AS mimeType,
                       duration_ms AS durationMs,
                       bytes,
                       sha256,
                       created_at AS createdAt
                FROM audio_assets
                ORDER BY id DESC
                LIMIT #{limit} OFFSET #{offset}
            """)
    List<AudioAsset> list(@Param("offset") int offset, @Param("limit") int limit);

    @Update("""
                UPDATE audio_assets
                SET local_path = #{localPath},
                    mime_type = #{mimeType},
                    duration_ms = #{durationMs},
                    bytes = #{bytes},
                    sha256 = #{sha256}
                WHERE id = #{id}
            """)
    int update(AudioAsset a);

    @Delete("DELETE FROM audio_assets WHERE id = #{id}")
    int deleteById(@Param("id") long id);

    // ✅ 外键校验常用：materials.audio_id -> audio_assets.id
    @Select("SELECT 1 FROM audio_assets WHERE id = #{id} LIMIT 1")
    Integer exists(@Param("id") long id);
}
