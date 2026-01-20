package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.AttemptAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
@Mapper
public interface AttemptAnswerMapper {

    @Insert("""
            <script>
              INSERT INTO attempt_answers(attempt_id, question_id, chosen_key, is_correct)
              VALUES
              <foreach collection="list" item="it" separator=",">
                (#{it.attemptId}, #{it.questionId}, #{it.chosenKey}, #{it.isCorrect})
              </foreach>
            </script>
            """)
    int batchInsert(@Param("list") List<AttemptAnswer> list);
}
