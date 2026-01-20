package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.user.StudentAbilityState;
import com.tencent.wxcloudrun.service.PracticeService;
import com.tencent.wxcloudrun.service.ability.AbilityEstimator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Service
public class PracticeServiceImpl implements PracticeService {

    private final StudentMapper studentMapper;
    private final QuestionMapper questionMapper;
    private final StudentAbilityStateMapper abilityStateMapper;

    public PracticeServiceImpl(StudentMapper studentMapper,
                               QuestionMapper questionMapper,
                               StudentAbilityStateMapper abilityStateMapper) {
        this.studentMapper = studentMapper;
        this.questionMapper = questionMapper;
        this.abilityStateMapper = abilityStateMapper;
    }

    @Override
    @Transactional
    public PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req) {
        if (studentId == null) throw new IllegalArgumentException("NO_AUTH");
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (req.getMaterialId() == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");
        if (req.getAnswers() == null || req.getAnswers().isEmpty()) throw new IllegalArgumentException("ANSWERS_EMPTY");

        // 1) 拉取材料下题目（你之前已经在 getAMaterial 用过 listByMaterialId）
        List<Question> questions = questionMapper.listByMaterialId(req.getMaterialId());
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("NO_QUESTIONS");

        Map<Long, Question> qMap = new HashMap<>();
        for (Question q : questions) qMap.put(q.getId(), q);

        // 2) 组装 correctByQid（并校验 questionId 属于该 material）
        Map<Long, Boolean> correctByQid = new HashMap<>();
        int correctCnt = 0;

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
        }

        //（可选）要求必须答完材料所有题，否则你需要定义“未答题”怎么算
        if (correctByQid.size() != questions.size()) {
            throw new IllegalArgumentException("ANSWER_COUNT_MISMATCH");
        }

        // 3) 读取学生当前 theta
        Student stu = studentMapper.findById(String.valueOf(studentId));
        if (stu == null) throw new IllegalArgumentException("STUDENT_NOT_FOUND");

        double thetaOld = (stu.getTheta() == null) ? 5.0 : stu.getTheta().doubleValue();

        // 4) 读取/初始化 var 与练习次数
        StudentAbilityState st = abilityStateMapper.findByStudentId(studentId);
        if (st == null) {
            // 初始 var：建议 4.0（sigma=2），count=0
            abilityStateMapper.initIfAbsent(studentId, 4.0, 0);
            st = abilityStateMapper.findByStudentId(studentId);
        }
        double varOld = (st.getThetaVar() == null) ? 4.0 : st.getThetaVar();

        // 5) 更新 (theta,var)
        AbilityEstimator.UpdateResult upd =
                AbilityEstimator.update(thetaOld, varOld, questions, correctByQid);

        double thetaNew = upd.thetaNew;
        double varNew = upd.varNew;

        // 6) 落库（需要你在 StudentMapper 增加 updateThetaById）
        studentMapper.updateThetaById(studentId, BigDecimal.valueOf(thetaNew));
        abilityStateMapper.updateState(studentId, varNew, (st.getPracticeCount() == null ? 0 : st.getPracticeCount()) + 1);

        PracticeSubmitResponse out = new PracticeSubmitResponse();
        out.setMaterialId(req.getMaterialId());
        out.setTotal(questions.size());
        out.setCorrect(correctCnt);
        out.setThetaOld(thetaOld);
        out.setThetaNew(thetaNew);
        out.setVarOld(varOld);
        out.setVarNew(varNew);
        out.setPracticeCountNew((st.getPracticeCount() == null ? 0 : st.getPracticeCount()) + 1);
        return out;
    }
}
