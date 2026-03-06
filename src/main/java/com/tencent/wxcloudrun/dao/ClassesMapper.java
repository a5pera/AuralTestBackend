package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.admin.ClassMaterialAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassMaterialQuestionAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassOneQuestionAccuracyDTO;
import com.tencent.wxcloudrun.model.user.Classes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
@Mapper
public interface ClassesMapper {
    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            """)
    List<Classes> listAll();

    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            WHERE is_active=1
            """)
    List<Classes> listActive();

    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            WHERE id=#{id}
            """)
    Classes findById(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET is_active=1
            WHERE id=#{id}
            """)
    int activate(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET is_active=0
            WHERE id=#{id}
            """)
    int deActivate(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET name=#{name}
            WHERE id=#{id}
            """)
    int updateName(@Param("id") Long id, @Param("name") String name);

    @Select("""
    SELECT
      #{classId}    AS classId,
      #{questionId} AS questionId,
      SUM(CASE WHEN t.is_correct = 1 THEN 1 ELSE 0 END) AS correctCnt,
      COUNT(1) AS attemptCnt,
      (SUM(CASE WHEN t.is_correct = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(1),0)) AS accuracy
    FROM (
      SELECT a.student_id, aa.is_correct
      FROM attempt_answers aa
      JOIN attempts a ON a.id = aa.attempt_id
      JOIN students s ON s.id = a.student_id
      JOIN student_roster r ON r.id = s.roster_id
      JOIN (
        SELECT a2.student_id, MAX(a2.id) AS max_attempt_id
        FROM attempt_answers aa2
        JOIN attempts a2 ON a2.id = aa2.attempt_id
        JOIN students s2 ON s2.id = a2.student_id
        JOIN student_roster r2 ON r2.id = s2.roster_id
        WHERE r2.class_id = #{classId}
          AND aa2.question_id = #{questionId}
        GROUP BY a2.student_id
      ) last1
        ON last1.student_id = a.student_id
       AND last1.max_attempt_id = a.id
      WHERE r.class_id = #{classId}
        AND aa.question_id = #{questionId}
    ) t
""")
    ClassOneQuestionAccuracyDTO statOneQuestionAccuracyLatestPerStudent(@Param("classId") Long classId,
                                                                        @Param("questionId") Long questionId);

    @Select("""
        SELECT
          m.id    AS materialId,
          m.title AS materialTitle,
          m.level AS materialLevel,
          CAST(
            SUM(CASE WHEN aa.is_correct = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(1), 0)
            AS DECIMAL(10,4)
          ) AS accuracy
        FROM (
          SELECT
            a2.student_id,
            a2.material_id,
            MAX(a2.id) AS latest_attempt_id
          FROM attempts a2
          JOIN students s2       ON s2.id = a2.student_id
          JOIN student_roster r2 ON r2.id = s2.roster_id
          WHERE r2.class_id = #{classId}
          GROUP BY a2.student_id, a2.material_id
        ) last1
        JOIN attempts a        ON a.id = last1.latest_attempt_id
        JOIN attempt_answers aa ON aa.attempt_id = a.id
        JOIN materials m       ON m.id = a.material_id
        WHERE m.is_active = 1
        GROUP BY m.id, m.title, m.level
        ORDER BY m.level ASC, m.id ASC
    """)
    List<ClassMaterialAccuracyDTO> statMaterialAccuracyByClassLatest(@Param("classId") Long classId);

    @Select("""
        SELECT
          #{materialId} AS materialId,
          q.id          AS questionId,
          q.q_order     AS qOrder,
          SUM(CASE WHEN aa.is_correct = 1 THEN 1 ELSE 0 END) AS correctCnt,
          COUNT(1) AS studentCnt,
          CAST(
            SUM(CASE WHEN aa.is_correct = 1 THEN 1 ELSE 0 END) / NULLIF(COUNT(1), 0)
            AS DECIMAL(10,4)
          ) AS accuracy
        FROM (
          SELECT
            a2.student_id,
            MAX(a2.id) AS latest_attempt_id
          FROM attempts a2
          JOIN students s2       ON s2.id = a2.student_id
          JOIN student_roster r2 ON r2.id = s2.roster_id
          WHERE r2.class_id = #{classId}
            AND a2.material_id = #{materialId}
          GROUP BY a2.student_id
        ) last1
        JOIN attempt_answers aa ON aa.attempt_id = last1.latest_attempt_id
        JOIN questions q        ON q.id = aa.question_id
        WHERE q.material_id = #{materialId}
        GROUP BY q.id, q.q_order
        ORDER BY q.q_order ASC, q.id ASC
    """)
    List<ClassMaterialQuestionAccuracyDTO> statQuestionAccuracyByClassLatestAttempt(
            @Param("classId") Long classId,
            @Param("materialId") Long materialId
    );
}
