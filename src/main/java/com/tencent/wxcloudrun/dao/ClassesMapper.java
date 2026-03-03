package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.Classes;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;
@Mapper
public interface ClassesMapper {
    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            """)
    List<Classes> listAll();

    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            WHERE is_active=1
            """)
    List<Classes> listActive();

    @Select("""
            SELECT id, name, is_active AS isActive, created_at AS createdAt, updated_at AS updatedAt
            FROM classes
            WHERE id=#{id}
            """)
    Classes findById(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET is_active=1
            WHERE id=#{id}
            """)
    int activate(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET is_active=0
            WHERE id=#{id}
            """)
    int deActivate(@Param("id") Long id);

    @Update("""
            UPDATE classes
            SET name=#{name}
            WHERE id=#{id}
            """)
    int updateName(@Param("id") Long id, @Param("name") String name);
}
