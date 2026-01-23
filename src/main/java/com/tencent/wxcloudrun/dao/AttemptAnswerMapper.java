package com.tencent.wxcloudrun.dao;

import com.tencent.wxcloudrun.model.user.AttemptAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

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

    @Select("""
                SELECT COUNT(DISTINCT aa.question_id)
                FROM attempt_answers aa
                JOIN attempts a ON a.id = aa.attempt_id
                WHERE a.student_id = #{studentId}
            """)
    long countUniquePracticedQuestions(@Param("studentId") Long studentId);

    @Select("""
        SELECT COUNT(1)
        FROM attempt_answers aa
        JOIN attempts a ON a.id = aa.attempt_id
        WHERE a.student_id = #{studentId}
    """)
    long countPracticedQuestions(@Param("studentId") Long studentId);


    @Select("""
            SELECT id, attempt_id AS attemptId, question_id AS questionId, chosen_key AS chosenKey, is_correct AS isCorrect
            FROM attempt_answer
            WHERE attempt_id = {attemptId}
            """)
    List<AttemptAnswer> findByAttemptId(@Param("attemptId") long attemptId);
}
