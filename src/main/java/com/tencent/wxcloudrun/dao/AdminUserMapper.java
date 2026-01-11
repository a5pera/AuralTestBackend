package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.auth.AdminUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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
}
