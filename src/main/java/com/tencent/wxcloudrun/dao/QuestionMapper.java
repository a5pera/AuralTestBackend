package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.db.RedoQuestionRow;
import com.tencent.wxcloudrun.model.quest.Question;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface QuestionMapper {

    @Insert("""
                INSERT INTO questions(material_id, q_order, stem, difficulty, correct_key)
                VALUES(#{materialId}, #{qOrder}, #{stem}, #{difficulty}, #{correctKey})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Question q);

    @Select("""
                SELECT id,
                       material_id AS materialId,
                       q_order AS qOrder,
                       stem,
                       difficulty,
                       correct_key AS correctKey,
                       created_at AS createdAt
                FROM questions
                WHERE id = #{id}
                LIMIT 1
            """)
    Question findById(@Param("id") long id);

    // 根据材料列出其所属的问题
    @Select("""
                SELECT id,
                       material_id AS materialId,
                       q_order AS qOrder,
                       stem,
                       difficulty,
                       correct_key AS correctKey,
                       created_at AS createdAt
                FROM questions
                WHERE material_id = #{materialId}
                ORDER BY q_order ASC
            """)
    List<Question> listByMaterialId(@Param("materialId") long materialId);

    @Select("""
        SELECT 
            id          AS qId,
            q_order     AS qOrder,
            correct_key AS correctKey
        FROM questions
        WHERE material_id = #{materialId}
        ORDER BY q_order ASC
    """)
    List<RedoQuestionRow> listRedoByMaterialId(@Param("materialId") Long materialId);

    // ✅ 不允许改 material_id / q_order（保证“绑定唯一”语义）
    @Update("""
                UPDATE questions
                SET stem = #{stem},
                    difficulty = #{difficulty},
                    correct_key = #{correctKey}
                WHERE id = #{id}
            """)
    int updateContent(Question q);

    @Update("""
        UPDATE questions
        SET stem = #{stem},
            difficulty = #{difficulty},
            correct_key = #{correctKey}
        WHERE id = #{id}
    """)
    int updateCoreById(@Param("id") Long id,
                       @Param("stem") String stem,
                       @Param("difficulty") BigDecimal difficulty,
                       @Param("correctKey") String correctKey);

    @Delete("""
                DELETE FROM questions
                WHERE id = #{id}
            """)
    int deleteById(@Param("id") long id);
}
