package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.StudentAbilityState;
import org.apache.ibatis.annotations.*;

@Mapper
public interface StudentAbilityStateMapper {

    @Insert("""
                INSERT INTO student_ability_state(student_id, theta_var, practice_count)
                VALUES(#{studentId}, #{thetaVar}, #{practiceCount})
            """)
    int insert(StudentAbilityState s);

    @Select("""
                SELECT student_id AS studentId,
                       theta_var   AS thetaVar,
                       practice_count AS practiceCount,
                       updated_at  AS updatedAt
                FROM student_ability_state
                WHERE student_id = #{studentId}
                LIMIT 1
            """)
    StudentAbilityState findByStudentId(@Param("studentId") Long studentId);

    @Select("""
                SELECT EXISTS(
                    SELECT 1 FROM student_ability_state WHERE student_id = #{studentId}
                )
            """)
    boolean exists(@Param("studentId") Long studentId);

    /**
     * 初始化：如果不存在则插入；存在则不覆盖（保留原来的 var/count）
     */
    @Insert("""
                INSERT INTO student_ability_state(student_id, theta_var, practice_count)
                VALUES(#{studentId}, #{thetaVar}, #{practiceCount})
                ON DUPLICATE KEY UPDATE
                  theta_var = theta_var,
                  practice_count = practice_count
            """)
    int initIfAbsent(@Param("studentId") Long studentId,
                     @Param("thetaVar") Double thetaVar,
                     @Param("practiceCount") Integer practiceCount);

    /**
     * 更新能力不确定性 & 练习次数（常用：提交一次材料后 count+1）
     */
    @Update("""
                UPDATE student_ability_state
                SET theta_var = #{thetaVar},
                    practice_count = #{practiceCount}
                WHERE student_id = #{studentId}
            """)
    int updateState(@Param("studentId") Long studentId,
                    @Param("thetaVar") Double thetaVar,
                    @Param("practiceCount") Integer practiceCount);

    /**
     * 原子自增练习次数（推荐用它，避免并发覆盖）
     */
    @Update("""
                UPDATE student_ability_state
                SET practice_count = practice_count + 1
                WHERE student_id = #{studentId}
            """)
    int incPracticeCount(@Param("studentId") Long studentId);

    /**
     * 只更新方差
     */
    @Update("""
                UPDATE student_ability_state
                SET theta_var = #{thetaVar}
                WHERE student_id = #{studentId}
            """)
    int updateThetaVar(@Param("studentId") Long studentId,
                       @Param("thetaVar") Double thetaVar);

    @Delete("""
                DELETE FROM student_ability_state
                WHERE student_id = #{studentId}
            """)
    int deleteByStudentId(@Param("studentId") Long studentId);
}
