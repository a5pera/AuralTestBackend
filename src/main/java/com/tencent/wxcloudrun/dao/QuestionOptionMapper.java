package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.dto.quest.RedoGetPracticeRequest;
import com.tencent.wxcloudrun.model.quest.QuestionOption;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionOptionMapper {

    @Insert("""
                INSERT INTO question_options(question_id, opt_key, content)
                VALUES(#{questionId}, #{optKey}, #{content})
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(QuestionOption opt);


    // 根据问题的id列出其所属的选项
    @Select("""
                SELECT id,
                       question_id AS questionId,
                       opt_key AS optKey,
                       content
                FROM question_options
                WHERE question_id = #{questionId}
                ORDER BY opt_key ASC
            """)
    List<QuestionOption> listByQuestionId(@Param("questionId") long questionId);

    @Select("""
                SELECT 
                    opt_key AS optKey,
                    content
                FROM question_options
                WHERE question_id = #{questionId}
                ORDER BY opt_key ASC
            """)
    List<RedoGetPracticeRequest.OptionDTO> listOptionDTOByQuestionId(@Param("questionId") Long questionId);

//    @Update("""
//                UPDATE question_options
//                SET content = #{content}
//                WHERE question_id = #{questionId} AND opt_key = #{optKey}
//            """)
//    int updateContent(@Param("questionId") long questionId,
//                      @Param("optKey") String optKey,
//                      @Param("content") String content);
//
//    @Delete("""
//                DELETE FROM question_options
//                WHERE question_id = #{questionId} AND opt_key = #{optKey}
//            """)
//    int deleteOne(@Param("questionId") long questionId,
//                  @Param("optKey") String optKey);

    @Insert("""
            INSERT INTO question_options(question_id, opt_key, content)
            VALUES(#{questionId}, #{optKey}, #{content})
            ON DUPLICATE KEY UPDATE
                content = VALUES(content)
            """)
    int upsert(@Param("questionId") Long questionId,
               @Param("optKey") String optKey,
               @Param("content") String content);

    @Delete("""
            <script>
            DELETE FROM question_options
            WHERE question_id = #{questionId}
              AND opt_key NOT IN
              <foreach collection="keys" item="k" open="(" separator="," close=")">
                #{k}
              </foreach>
            </script>
            """)
    int deleteNotInKeys(@Param("questionId") Long questionId,
                        @Param("keys") List<String> keys);

    @Delete("""
                DELETE FROM question_options
                WHERE question_id = #{questionId}
            """)
    int deleteByQuestionId(@Param("questionId") long questionId);
}
