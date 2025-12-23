package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.AvatarAsset;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AvatarAssetMapper {
    @Select("""
                SELECT student_id AS studentId, source_type AS sourceType, local_path AS localPath, mime_type AS mimeType
                FROM avatar_assets
                WHERE student_id = #{studentId}
                LIMIT 1
            """)
    AvatarAsset findByStudentId(@Param("studentId") Long studentId);

    @Insert("""
                INSERT INTO avatar_assets(student_id, source_type, local_path, mime_type)
                VALUES(#{studentId}, #{sourceType}, #{localPath}, #{mimeType})
            """)
    int insert(AvatarAsset a);
}
