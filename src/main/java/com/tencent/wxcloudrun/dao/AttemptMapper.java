package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.Attempt;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
@Mapper
public interface AttemptMapper {

    @Insert("""
                INSERT INTO attempts(student_id, material_id, total_q, theta_before, theta_after, started_at, submitted_at)
                VALUES(#{studentId}, #{materialId}, #{totalQ}, #{thetaBefore}, #{thetaAfter}, #{startedAt}, #{submittedAt})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Attempt a);
}
