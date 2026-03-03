package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.Student;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface StudentMapper {
    @Select("""
            SELECT id, roster_id, student_no AS studentNo, name, college, wechat_openid AS wechatOpenid, theta, bound_at AS boundAt, class_id AS classId
            FROM students WHERE id = #{studentId}
            """
    )
    Student findById(@Param("studentId") String studentId);

    @Select("""
              SELECT id, student_no AS studentNo, name, college, wechat_openid AS wechatOpenid, theta, class_id AS classId
              FROM students WHERE wechat_openid = #{openid} LIMIT 1
            """)
    Student findByOpenid(@Param("openid") String openid);

    @Select("""
              SELECT id, student_no AS studentNo, name, college, wechat_openid AS wechatOpenid, theta, class_id AS classId
              FROM students WHERE student_no = #{studentNo} LIMIT 1
            """)
    Student findByStudentNo(@Param("studentNo") String studentNo);

    @Select("""
                SELECT s.id,
                       s.roster_id      AS rosterId,
                       s.student_no     AS studentNo,
                       s.name,
                       s.college,
                       s.wechat_openid  AS wechatOpenid,
                       s.theta,
                       s.bound_at       AS boundAt,
                       s.class_id AS classId,
                       COALESCE(st.practice_count, 0) AS practiceCount
                FROM students s
                LEFT JOIN student_ability_state st ON st.student_id = s.id
                ORDER BY s.theta DESC, s.id ASC
                LIMIT #{limit} OFFSET #{offset}
            """)
    List<Student> listLeaderboard(@Param("limit") int limit, @Param("offset") int offset);

    @Select("""
                SELECT COUNT(1)
                FROM students
            """)
    long countLeaderboard();

    @Insert("""
              INSERT INTO students(roster_id, student_no, name, college, wechat_openid, theta, bound_at, class_id)
              VALUES(#{rosterId}, #{studentNo}, #{name}, #{college}, #{wechatOpenid}, #{theta}, NOW(), #{classId})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student s);

    @Update("""
              UPDATE students SET wechat_openid = #{openid}, bound_at = NOW()
              WHERE id = #{studentId}
            """)
    int bindOpenid(@Param("studentId") Long studentId, @Param("openid") String openid);

    @Update("""
                UPDATE students
                SET theta = #{theta}
                WHERE id = #{studentId}
            """)
    int updateThetaById(@Param("studentId") Long studentId,
                        @Param("theta") BigDecimal theta);
}
