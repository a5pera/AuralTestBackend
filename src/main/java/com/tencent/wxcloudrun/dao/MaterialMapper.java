package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.db.RedoMaterialRow;
import com.tencent.wxcloudrun.dto.quest.MaterialIdAndLevel;
import com.tencent.wxcloudrun.model.quest.Material;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface MaterialMapper {

    @Insert("""
                INSERT INTO materials(title, level, transcript, audio_id, is_active)
                VALUES(#{title}, #{level}, #{transcript}, #{audioId}, #{isActive})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Material m);

    @Select("""
                SELECT id, title, level, transcript, audio_id AS audioId, is_active AS isActive,
                       created_at AS createdAt, updated_at AS updatedAt
                FROM materials
                WHERE id = #{id}
                LIMIT 1
            """)
    Material findById(@Param("id") long id);

    @Select("""
                SELECT id, title, level, transcript, audio_id AS audioId, is_active AS isActive,
                       created_at AS createdAt, updated_at AS updatedAt
                FROM materials
                WHERE is_active = 1
                ORDER BY id DESC
                LIMIT #{limit} OFFSET #{offset}
            """)
    List<Material> listActive(@Param("offset") int offset, @Param("limit") int limit);

    @Select("""
                SELECT id, title, level, transcript, audio_id AS audioId, is_active AS isActive,
                       created_at AS createdAt, updated_at AS updatedAt
                FROM materials
                ORDER BY id DESC
            """
    )
    List<Material> listAll();

    @Select("""
                SELECT m.id AS materialId, m.level
                FROM materials m
                WHERE m.is_active = 1
                  AND NOT EXISTS (
                    SELECT 1
                    FROM attempts a
                    WHERE a.student_id = #{studentId}
                      AND a.material_id = m.id
                  )
                ORDER BY m.level DESC
            """)
    List<MaterialIdAndLevel> listIdAndLevel(@Param("studentId") Long studentId);

    @Select({
            "<script>",
            "SELECT id, title, level, transcript, audio_id AS audioId, is_active AS isActive,",
            "       created_at AS createdAt, updated_at AS updatedAt",
            "FROM materials",
            "WHERE id IN",
            "<foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "  #{id}",
            "</foreach>",
            "</script>"
    })
    List<Material> listByIds(@Param("ids") List<Long> ids);

    @Select("""
                SELECT 
                    m.id            AS materialId,
                    m.title         AS materialTitle,
                    m.transcript    AS materialTranscript,
                    m.level         AS materialLevel,
                    m.audio_id      AS audioId,
                    a.local_path    AS audioPath,
                    a.mime_type     AS audioType
                FROM materials m
                JOIN audio_assets a ON a.id = m.audio_id
                WHERE m.id = #{materialId}
                  AND m.is_active = 1
                LIMIT 1
            """)
    RedoMaterialRow findRedoRowById(@Param("materialId") Long materialId);

    @Update("""
                UPDATE materials
                SET title = #{title},
                    level = #{level},
                    transcript = #{transcript},
                    audio_id = #{audioId},
                    is_active = #{isActive}
                WHERE id = #{id}
            """)
    int updateById(Material m);

    @Update("""
                UPDATE materials
                SET audio_id = #{audioId}
                WHERE id = #{id}
            """)
    int updateAudioIdByMaterialId(@Param("audioId") long audioId, @Param("id") long id);

    @Update("""
                UPDATE materials
                SET is_active = 0
                WHERE id = #{id}
            """)
    int softDelete(@Param("id") long id);

    @Update("""
                UPDATE materials
                SET is_active = 1
                WHERE id = #{id}
            """)
    int active(@Param("id") long id);

    @Delete("""
                DELETE FROM materials
                WHERE id = #{id}
            """)
    int deleteHard(@Param("id") long id);
}