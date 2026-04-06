package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.db.StudentPracticeRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminPracticeMapper {
    /**
     * 查询：该学生每个材料“最新一次 attempt”下的所有题目作答明细
     */
    @Select("""
        SELECT
          s.id AS studentId,
          s.student_no AS studentNo,
          s.name AS studentName,

          m.id AS materialId,
          m.title AS materialTitle,

          q.id AS questionId,
          q.q_order AS qOrder,
          q.correct_key AS correctKey,

          aa.chosen_key AS selectedKey,
          aa.is_correct AS isCorrect
        FROM (
          SELECT material_id, MAX(id) AS latest_attempt_id
          FROM attempts
          WHERE student_id = #{studentId}
          GROUP BY material_id
        ) la
        JOIN attempts a          ON a.id = la.latest_attempt_id
        JOIN students s          ON s.id = a.student_id
        JOIN materials m         ON m.id = a.material_id
        JOIN attempt_answers aa  ON aa.attempt_id = a.id
        JOIN questions q         ON q.id = aa.question_id
        ORDER BY m.id ASC, q.q_order ASC, q.id ASC
    """)
    List<StudentPracticeRow> listLatestPracticeRowsByStudentId(@Param("studentId") Long studentId);
}
