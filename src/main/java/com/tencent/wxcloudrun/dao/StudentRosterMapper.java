package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.StudentRoster;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
