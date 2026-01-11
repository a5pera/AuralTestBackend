package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.quest.QuestionOption;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface QuestionService {
    Question createQuestion(long materialId,
                            int qOrder,
                            String stem,
                            BigDecimal difficulty,
                            String correctKey,
                            Map<String, String> options);

    Question getQuestion(long questionId);

    List<Question> listQuestionsByMaterial(long materialId);

    List<QuestionOption> listOptions(long questionId);

    Question updateQuestion(long questionId,
                            String stem,
                            BigDecimal difficulty,
                            String correctKey,
                            Map<String, String> optionsOrNull);

    void deleteQuestion(long questionId);
}
