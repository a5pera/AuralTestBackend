package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.user.Attempt;
import com.tencent.wxcloudrun.model.user.AttemptAnswer;
import com.tencent.wxcloudrun.model.user.StudentAbilityState;
import com.tencent.wxcloudrun.service.PracticeService;
import com.tencent.wxcloudrun.service.ability.AbilityEstimator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final StudentMapper studentMapper;
    private final QuestionMapper questionMapper;
    private final StudentAbilityStateMapper abilityStateMapper;
    private final AttemptMapper attemptMapper;
    private final AttemptAnswerMapper attemptAnswerMapper;

    public PracticeServiceImpl(StudentMapper studentMapper,
                               QuestionMapper questionMapper,
                               StudentAbilityStateMapper abilityStateMapper,
                               AttemptMapper attemptMapper,
                               AttemptAnswerMapper attemptAnswerMapper) {
        this.studentMapper = studentMapper;
        this.questionMapper = questionMapper;
        this.abilityStateMapper = abilityStateMapper;
        this.attemptMapper = attemptMapper;
        this.attemptAnswerMapper = attemptAnswerMapper;
    }

    @Override
    @Transactional
    public PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req) {
        if (studentId == null) throw new IllegalArgumentException("NO_AUTH");
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (req.getMaterialId() == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");
        if (req.getAnswers() == null || req.getAnswers().isEmpty()) throw new IllegalArgumentException("ANSWERS_EMPTY");

        // 1) 拉取题目
        List<Question> questions = questionMapper.listByMaterialId(req.getMaterialId());
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("NO_QUESTIONS");

        Map<Long, Question> qMap = new HashMap<>();
        for (Question q : questions) qMap.put(q.getId(), q);

        // 2) correctByQid + 统计 + 回包 answers（先准备好）
        Map<Long, Boolean> correctByQid = new HashMap<>();
        int correctCnt = 0;

        List<PracticeSubmitResponse.CorrectAnswerDTO> answerOut = new ArrayList<>();

        for (PracticeSubmitRequest.AnswerDTO a : req.getAnswers()) {
            if (a == null || a.getQuestionId() == null) throw new IllegalArgumentException("QUESTION_ID_MISSING");
            if (isBlank(a.getSelectedKey())) throw new IllegalArgumentException("SELECTED_KEY_MISSING");

            Question q = qMap.get(a.getQuestionId());
            if (q == null) throw new IllegalArgumentException("QUESTION_NOT_IN_MATERIAL");

            String sel = a.getSelectedKey().trim().toUpperCase();
            String ans = (q.getCorrectKey() == null) ? "" : q.getCorrectKey().trim().toUpperCase();

            boolean ok = sel.equals(ans);
            correctByQid.put(q.getId(), ok);
            if (ok) correctCnt++;

            PracticeSubmitResponse.CorrectAnswerDTO dto = new PracticeSubmitResponse.CorrectAnswerDTO();
            dto.setQuestionId(q.getId());
            dto.setCorrectAnswer(ans);
            dto.setSelectedAnswer(sel);
            dto.setIsCorrect(ok);
            answerOut.add(dto);
        }

        if (correctByQid.size() != questions.size()) {
            throw new IllegalArgumentException("ANSWER_COUNT_MISMATCH");
        }

        // 3) 读取学生 theta
        Student stu = studentMapper.findById(String.valueOf(studentId));
        if (stu == null) throw new IllegalArgumentException("STUDENT_NOT_FOUND");
        double thetaOld = (stu.getTheta() == null) ? 5.0 : stu.getTheta().doubleValue();

        // 4) 读取/初始化 var 与次数
        StudentAbilityState st = abilityStateMapper.findByStudentId(studentId);
        if (st == null) {
            abilityStateMapper.initIfAbsent(studentId, 4.0, 0);
            st = abilityStateMapper.findByStudentId(studentId);
        }
        double varOld = (st.getThetaVar() == null) ? 4.0 : st.getThetaVar();
        int practiceCountOld = (st.getPracticeCount() == null) ? 0 : st.getPracticeCount();

        // 5) 更新 (theta,var)
        AbilityEstimator.UpdateResult upd =
                AbilityEstimator.update(thetaOld, varOld, questions, correctByQid);

        double thetaNew = upd.thetaNew;
        double varNew = upd.varNew;

        // 6) 写入 attempts（先写 attempt，拿到 attemptId）
        LocalDateTime now = LocalDateTime.now();

        Attempt attempt = new Attempt();
        attempt.setStudentId(studentId);
        attempt.setMaterialId(req.getMaterialId());
        attempt.setTotalQ(questions.size());
        attempt.setThetaBefore(BigDecimal.valueOf(thetaOld));
        attempt.setThetaAfter(BigDecimal.valueOf(thetaNew));
        attempt.setStartedAt(now);
        attempt.setSubmittedAt(now);

        attemptMapper.insert(attempt);
        Long attemptId = attempt.getId();
        if (attemptId == null) throw new IllegalStateException("ATTEMPT_INSERT_FAIL");

        // 7) 写入 attempt_answers（每题一条）
        List<AttemptAnswer> rows = new ArrayList<>(answerOut.size());
        for (PracticeSubmitResponse.CorrectAnswerDTO dto : answerOut) {
            AttemptAnswer row = new AttemptAnswer();
            row.setAttemptId(attemptId);
            row.setQuestionId(dto.getQuestionId());
            row.setChosenKey(dto.getSelectedAnswer());  // A/B/C/D
            row.setIsCorrect(Boolean.TRUE.equals(dto.getIsCorrect()));
            rows.add(row);
        }
        attemptAnswerMapper.batchInsert(rows);

        // 8) 落库 theta 与能力状态
        studentMapper.updateThetaById(studentId, BigDecimal.valueOf(thetaNew));
        abilityStateMapper.updateState(studentId, varNew, practiceCountOld + 1);

        // 9) 返回新 DTO
        PracticeSubmitResponse out = new PracticeSubmitResponse();
        out.setMaterialId(req.getMaterialId());
        out.setTotal(questions.size());
        out.setCorrect(correctCnt);
        out.setThetaOld(thetaOld);
        out.setThetaNew(thetaNew);
        out.setVarOld(varOld);
        out.setVarNew(varNew);
        out.setPracticeCountNew(practiceCountOld + 1);
        out.setAnswers(answerOut);
        return out;
    }

    @Override
    public List<Attempt> getAttemptsByStudentId(Long studentId) {
        if (studentId == null) throw new IllegalArgumentException("LOST_STUDENT_ID");
        return attemptMapper.listAttemptByStudentId(studentId);
    }

    @Override
    public List<AttemptAnswer> getDetailByAttemptId(Long attemptId) {
        if (attemptId == null) throw new IllegalArgumentException("LOST_ATTEMPT_ID");
        return attemptAnswerMapper.findByAttemptId(attemptId);
    }
}
