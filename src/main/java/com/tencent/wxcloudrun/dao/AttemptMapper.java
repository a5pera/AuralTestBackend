package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.Attempt;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AttemptMapper {

    @Insert("""
                INSERT INTO attempts(student_id, material_id, total_q, theta_before, theta_after, started_at, submitted_at)
                VALUES(#{studentId}, #{materialId}, #{totalQ}, #{thetaBefore}, #{thetaAfter}, #{startedAt}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attempt a);

    @Select("""
                SELECT id, student_id AS studentId, material_id AS materialId, total_q AS totalQ, theta_before AS thetaBefore,
                theta_after AS thetaAfter, started_at as startedAt, submitted_at AS submittedAt
                FROM attempts
            """)
    List<Attempt> listAttempt();

    @Select("""
                SELECT id, material_id AS materialId, total_q AS totalQ, theta_before AS thetaBefore,
                    theta_after AS thetaAfter, started_at as startedAt, submitted_at AS submittedAt
                FROM attempts
                WHERE student_id=#{studentId}
            """)
    List<Attempt> listAttemptByStudentId(@Param("studentId") long studentId);

    @Select("""
                SELECT 1
                FROM attempts
                WHERE student_id = #{studentId}
                  AND material_id = #{materialId}
                LIMIT 1
            """)
    Integer existsByStudentAndMaterial(@Param("studentId") Long studentId,
                                       @Param("materialId") Long materialId);

    @Select("""
                SELECT COUNT(1)
                FROM attempts
                WHERE student_id = #{studentId} AND material_id = #{materialId}
            """)
    int countByStudentAndMaterial(@Param("studentId") Long studentId,
                                  @Param("materialId") Long materialId);

    @Select("""
                SELECT COUNT(DISTINCT material_id)
                FROM attempts
                WHERE student_id = #{studentId}
            """)
    long countDistinctMaterialByStudentId(@Param("studentId") Long studentId);
}
