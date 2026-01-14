package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.Student;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentMapper {
    @Select("""
            SELECT id, roster_id, student_no, name, college, wechat_openid, theta, boundAt
            FROM students WHERE id = #{studentId}
            """
    )
    Student findById(@Param("studentId") String studentId);

    @Select("""
              SELECT id, student_no AS studentNo, name, college, wechat_openid AS wechatOpenid, theta
              FROM students WHERE wechat_openid = #{openid} LIMIT 1
            """)
    Student findByOpenid(@Param("openid") String openid);

    @Select("""
              SELECT id, student_no AS studentNo, name, college, wechat_openid AS wechatOpenid, theta
              FROM students WHERE student_no = #{studentNo} LIMIT 1
            """)
    Student findByStudentNo(@Param("studentNo") String studentNo);

    @Insert("""
              INSERT INTO students(roster_id, student_no, name, college, wechat_openid, theta, bound_at)
              VALUES(#{rosterId}, #{studentNo}, #{name}, #{college}, #{wechatOpenid}, #{theta}, NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Student s);

    @Update("""
              UPDATE students SET wechat_openid = #{openid}, bound_at = NOW()
              WHERE id = #{studentId}
            """)
    int bindOpenid(@Param("studentId") Long studentId, @Param("openid") String openid);
}
