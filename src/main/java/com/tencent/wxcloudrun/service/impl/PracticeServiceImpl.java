package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.db.RedoMaterialRow;
import com.tencent.wxcloudrun.dto.db.RedoQuestionRow;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDTO;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDetailedDTO;
import com.tencent.wxcloudrun.dto.quest.MaterialIdAndLevel;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.dto.quest.RedoGetPracticeRequest;
import com.tencent.wxcloudrun.model.quest.Material;
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
    private final MaterialMapper materialMapper;
    private final QuestionOptionMapper questionOptionMapper;

    public PracticeServiceImpl(StudentMapper studentMapper,
                               QuestionMapper questionMapper,
                               StudentAbilityStateMapper abilityStateMapper,
                               AttemptMapper attemptMapper,
                               AttemptAnswerMapper attemptAnswerMapper,
                               MaterialMapper materialMapper,
                               QuestionOptionMapper questionOptionMapper) {
        this.studentMapper = studentMapper;
        this.questionMapper = questionMapper;
        this.abilityStateMapper = abilityStateMapper;
        this.attemptMapper = attemptMapper;
        this.attemptAnswerMapper = attemptAnswerMapper;
        this.materialMapper = materialMapper;
        this.questionOptionMapper = questionOptionMapper;
    }

    @Override
    @Transactional
    public PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req) {
        if (studentId == null) throw new IllegalArgumentException("NO_AUTH");
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (req.getMaterialId() == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");
        if (req.getAnswers() == null || req.getAnswers().isEmpty()) throw new IllegalArgumentException("ANSWERS_EMPTY");

        // 0) 是否练习过该材料：练习过 -> 不影响分数
        boolean practicedBefore = attemptMapper.countByStudentAndMaterial(studentId, req.getMaterialId()) > 0;

        // 1) 拉取题目
        List<Question> questions = questionMapper.listByMaterialId(req.getMaterialId());
        if (questions == null || questions.isEmpty()) throw new IllegalArgumentException("NO_QUESTIONS");

        Map<Long, Question> qMap = new HashMap<>();
        for (Question q : questions) qMap.put(q.getId(), q);

        // 2) correctByQid + 统计 + 回包 answers
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

        // 5) 更新 (theta,var) —— 若已练习过则不更新
        double thetaNew;
        double varNew;
        int practiceCountNew;

        if (practicedBefore) {
            thetaNew = thetaOld;
            varNew = varOld;
            practiceCountNew = practiceCountOld; // 不计入能力更新次数
        } else {
            AbilityEstimator.UpdateResult upd =
                    AbilityEstimator.update(thetaOld, varOld, questions, correctByQid);

            thetaNew = upd.thetaNew;
            varNew = upd.varNew;
            practiceCountNew = practiceCountOld + 1;
        }

        // 6) 写入 attempts（不管是否重复练习，都记一条；且重复练习时 before==after）
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
            row.setChosenKey(dto.getSelectedAnswer());
            row.setIsCorrect(Boolean.TRUE.equals(dto.getIsCorrect()));
            rows.add(row);
        }
        attemptAnswerMapper.batchInsert(rows);

        // 8) 落库 theta 与能力状态 —— 仅首次练习才更新
        if (!practicedBefore) {
            studentMapper.updateThetaById(studentId, BigDecimal.valueOf(thetaNew));
            abilityStateMapper.updateState(studentId, varNew, practiceCountNew);
        }

        // 9) 返回
        PracticeSubmitResponse out = new PracticeSubmitResponse();
        out.setMaterialId(req.getMaterialId());
        out.setTotal(questions.size());
        out.setCorrect(correctCnt);
        out.setThetaOld(thetaOld);
        out.setThetaNew(thetaNew);
        out.setVarOld(varOld);
        out.setVarNew(varNew);
        out.setPracticeCountNew(practiceCountNew);
        out.setAnswers(answerOut);
        return out;
    }

    @Override
    public List<PracticeAttemptDTO> getAttemptsByStudentId(Long studentId) {
        if (studentId == null) throw new IllegalArgumentException("LOST_STUDENT_ID");
        List<Attempt> attempts = attemptMapper.listAttemptByStudentId(studentId);
        List<PracticeAttemptDTO> out = new ArrayList<>();
        for (Attempt attempt : attempts) {
            PracticeAttemptDTO item = new PracticeAttemptDTO();
            item.setAttemptId(attempt.getId());
            Material m = materialMapper.findById(attempt.getMaterialId());
            item.setMaterialId(m.getId());
            item.setMaterialTitle(m.getTitle());
            item.setThetaBefore(attempt.getThetaBefore());
            item.setThetaAfter(attempt.getThetaAfter());
            item.setSubmittedAt(attempt.getSubmittedAt());
            out.add(item);
        }
        return out;
    }

    @Override
    public List<PracticeAttemptDetailedDTO> getDetailByAttemptId(Long attemptId) {
        if (attemptId == null) throw new IllegalArgumentException("LOST_ATTEMPT_ID");
        List<AttemptAnswer> attemptAnswers = attemptAnswerMapper.findByAttemptId(attemptId);
        List<PracticeAttemptDetailedDTO> out = new ArrayList<>();
        for (AttemptAnswer attemptAnswer : attemptAnswers) {
            PracticeAttemptDetailedDTO item = new PracticeAttemptDetailedDTO();
            Question q = questionMapper.findById(attemptAnswer.getQuestionId());
            item.setQuestionId(q.getId());
            item.setId(attemptAnswer.getId());
            item.setAttemptId(attemptAnswer.getAttemptId());
            item.setCorrectKey(q.getCorrectKey());
            item.setIsCorrect(attemptAnswer.getIsCorrect());
            item.setChosenKey(attemptAnswer.getChosenKey());
            out.add(item);
        }
        return out;
    }

    @Override
    public List<Material> recommendTop5Materials(long studentId) {
        // 1) 取学生能力 theta
        Student stu = studentMapper.findById(String.valueOf(studentId));
        if (stu == null) throw new IllegalArgumentException("STUDENT_NOT_FOUND");
        double theta = (stu.getTheta() == null) ? 5.0 : stu.getTheta().doubleValue();

        // 2) 取方差 var（可选：用于控制推荐“宽度”）
        StudentAbilityState st = abilityStateMapper.findByStudentId(studentId);
        double var = (st == null || st.getThetaVar() == null) ? 4.0 : st.getThetaVar();

        // 3) 候选：未练习过 + active
        List<MaterialIdAndLevel> cands = materialMapper.listIdAndLevel(studentId);
        if (cands == null || cands.isEmpty()) return List.of();

        // 4) 计算每个候选的匹配分并排序
        final double beta = 1.0; // 1PL 斜率/尺度，越大越“钝”
        // sigma 控制“围绕 theta 推荐的范围”，var 越大 -> 初期推荐更宽
        double sigma = clamp(0.6 + 0.15 * Math.sqrt(Math.max(var, 0.0)), 0.6, 1.2);

        List<Scored> scored = new ArrayList<>(cands.size());
        for (MaterialIdAndLevel it : cands) {
            if (it == null || it.getMaterialId() == null || it.getLevel() == null) continue;

            long id = it.getMaterialId();
            double b = it.getLevel().doubleValue();

            // 做对概率 p(θ,b)
            double p = sigmoid((theta - b) / beta);

            // Fisher information: p(1-p)/beta^2，最大为 0.25/beta^2（在 p=0.5）
            double info = (p * (1.0 - p)) / (beta * beta);
            double infoNorm = info / (0.25 / (beta * beta)); // 归一化到 [0,1]

            // 距离匹配（高斯权重），离 theta 越近越大
            double z = (b - theta) / sigma;
            double closeness = Math.exp(-0.5 * z * z); // (0,1]

            // 综合得分：你可以调权重
            double score = 0.7 * closeness + 0.3 * infoNorm;

            scored.add(new Scored(id, score));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 5) 取 top 5
        List<Long> topIds = scored.stream()
                .limit(5)
                .map(x -> x.id)
                .toList();

        // 6) 查详情并返回
        // 推荐用批量查，避免 5 次 findById
        List<Material> materials = materialMapper.listByIds(topIds);

        // 保持输出顺序与 topIds 一致（listByIds 可能乱序）
        Map<Long, Material> map = new HashMap<>();
        for (Material m : materials) map.put(m.getId(), m);

        List<Material> out = new ArrayList<>();
        for (Long id : topIds) {
            Material m = map.get(id);
            if (m != null) out.add(m);
        }
        return out;
    }

    @Override
    public RedoGetPracticeRequest getRedoMaterial(Long studentId, Long materialId) {
        if (studentId == null) throw new IllegalArgumentException("NO_AUTH");
        if (materialId == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");

        // 可选：没做过不让看正确答案（你不想限制就删掉这段）
        Integer ok = attemptMapper.existsByStudentAndMaterial(studentId, materialId);
        if (ok == null) throw new IllegalArgumentException("NOT_PRACTICED");

        RedoMaterialRow row = materialMapper.findRedoRowById(materialId);
        if (row == null) throw new IllegalArgumentException("MATERIAL_NOT_FOUND");

        List<RedoQuestionRow> qs = questionMapper.listRedoByMaterialId(materialId);
        if (qs == null || qs.isEmpty()) throw new IllegalArgumentException("NO_QUESTIONS");

        RedoGetPracticeRequest out = new RedoGetPracticeRequest();
        out.setMaterialId(row.getMaterialId());
        out.setMaterialTitle(row.getMaterialTitle());
        out.setMaterialTranscript(row.getMaterialTranscript());
        out.setMaterialLevel(row.getMaterialLevel());
        out.setAudioId(row.getAudioId());
        out.setAudioPath(row.getAudioPath());   // audio_assets.local_path
        out.setAudioType(row.getAudioType());   // audio_assets.mime_type

        List<RedoGetPracticeRequest.QuestionPracticeDTO> qDtos = new ArrayList<>(qs.size());
        for (RedoQuestionRow q : qs) {
            RedoGetPracticeRequest.QuestionPracticeDTO qdto = new RedoGetPracticeRequest.QuestionPracticeDTO();
            qdto.setQId(q.getQId());
            qdto.setQOrder(q.getQOrder());
            qdto.setCorrectKey(q.getCorrectKey());

            List<RedoGetPracticeRequest.OptionDTO> opts =
                    questionOptionMapper.listOptionDTOByQuestionId(q.getQId());
            qdto.setOptions(opts == null ? List.of() : opts);

            qDtos.add(qdto);
        }

        out.setQuestions(qDtos);
        return out;
    }

    private static double sigmoid(double x) {
        x = Math.max(-20.0, Math.min(20.0, x));
        return 1.0 / (1.0 + Math.exp(-x));
    }

    private static double clamp(double v, double lo, double hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private static class Scored {
        final long id;
        final double score;
        Scored(long id, double score) { this.id = id; this.score = score; }
    }
}
