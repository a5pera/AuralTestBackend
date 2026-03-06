package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.admin.ClassStudentCountDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentDTO;
import com.tencent.wxcloudrun.model.auth.StudentRoster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface StudentRosterMapper {
    @Select("""
                SELECT id, student_no AS studentNo, name, college, is_active AS isActive, class_id AS classId
                FROM student_roster
                WHERE student_no = #{studentNo} AND name = #{name} AND college = #{college} AND is_active = 1
                LIMIT 1
            """)
    StudentRoster findMatch(@Param("studentNo") String studentNo,
                            @Param("name") String name,
                            @Param("college") String college);

    /**
     * 班级学生列表（包含未绑定）
     * practicedQuestionCount：按 attempt_answers 行数统计（含重复）
     */
    @Select("""
        SELECT
            r.id              AS rosterId,
            s.id              AS studentId,
            CASE
              WHEN s.id IS NULL OR s.wechat_openid IS NULL THEN 'UNBOUND'
              ELSE 'BOUND'
            END               AS accountStatus,
            r.name            AS name,
            r.student_no      AS studentNo,
            r.college         AS college,
            COALESCE(pq.cnt, 0) AS practicedQuestionCount,
            s.theta           AS theta
        FROM student_roster r
        LEFT JOIN students s
               ON s.roster_id = r.id
        LEFT JOIN (
            SELECT a.student_id, COUNT(aa.question_id) AS cnt
            FROM attempts a
            JOIN attempt_answers aa ON aa.attempt_id = a.id
            GROUP BY a.student_id
        ) pq
               ON pq.student_id = s.id
        WHERE r.class_id = #{classId}
        ORDER BY r.student_no ASC
        LIMIT #{limit} OFFSET #{offset}
    """)
    List<ClassStudentDTO> listClassStudents(@Param("classId") Long classId,
                                            @Param("limit") Integer limit,
                                            @Param("offset") Integer offset);

    /**
     * 班级人数统计：总人数（roster）+ 已绑定人数（students）
     */
    @Select("""
        SELECT
            #{classId} AS classId,
            (SELECT COUNT(1) FROM student_roster r WHERE r.class_id = #{classId}) AS total,
            (SELECT COUNT(1)
               FROM student_roster r
               JOIN students s ON s.roster_id = r.id
              WHERE r.class_id = #{classId}
                AND s.wechat_openid IS NOT NULL) AS bound
    """)
    ClassStudentCountDTO countClassStudents(@Param("classId") Long classId);
}
