package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.AdminUser;
import org.apache.ibatis.annotations.*;

@Mapper
public interface AdminUserMapper {

    @Select("""
              SELECT id, username, password_hash AS passwordHash, display_name AS displayName,
                     is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
              FROM admin_users
              WHERE username = #{username}
              LIMIT 1
            """)
    AdminUser findByUsername(@Param("username") String username);

    @Select("""
              SELECT id, username, password_hash AS passwordHash, display_name AS displayName,
                     is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
              FROM admin_users
              WHERE id = #{id}
              LIMIT 1
            """)
    AdminUser findById(@Param("id") long id);

    @Insert("""
            INSERT INTO admin_users
              (username, password_hash, display_name, is_active, created_at, updated_at)
            VALUES
              (#{u.username}, #{u.passwordHash}, #{u.displayName}, #{u.isActive}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "u.id", keyColumn = "id")
    int insert(@Param("u") AdminUser u);
}
