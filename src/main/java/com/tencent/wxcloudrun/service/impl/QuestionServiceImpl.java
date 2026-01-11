package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.MaterialMapper;
import com.tencent.wxcloudrun.dao.QuestionMapper;
import com.tencent.wxcloudrun.dao.QuestionOptionMapper;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.quest.QuestionOption;
import com.tencent.wxcloudrun.service.QuestionService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.*;

@Service
public class QuestionServiceImpl implements QuestionService {

    @Resource
    private MaterialMapper materialMapper;            // ✅ 外键校验用
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper optionMapper;

    @Override
    @Transactional
    public Question createQuestion(long materialId, int qOrder, String stem, BigDecimal difficulty,
                                   String correctKey, Map<String, String> options) {

        // 1) 外键：material 必须存在
        Material material = materialMapper.findById(materialId);
        if (material == null) {
            throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        }

        // 2) 参数校验
        if (isBlank(stem) || difficulty == null || isBlank(correctKey) || options == null || options.isEmpty()) {
            throw new IllegalArgumentException("MISSING_PARAMS");
        }

        correctKey = normKey(correctKey);
        validateOptions(options, correctKey);

        // 3) 插入 question（uk_question_material_order 会约束同 material 下 q_order 唯一）
        Question q = new Question();
        q.setMaterialId(materialId);
        q.setQOrder(qOrder);
        q.setStem(stem);
        q.setDifficulty(difficulty);
        q.setCorrectKey(correctKey);

        try {
            questionMapper.insert(q);
        } catch (DuplicateKeyException e) {
            // 同一 material 下 q_order 冲突
            throw new IllegalArgumentException("QUESTION_ORDER_CONFLICT");
        }

        // 4) 插入 options（uk_option_question_key 约束同 question 下 opt_key 唯一）
        for (Map.Entry<String, String> en : options.entrySet()) {
            String k = normKey(en.getKey());
            String v = en.getValue();

            QuestionOption opt = new QuestionOption();
            opt.setQuestionId(q.getId());
            opt.setOptKey(k);
            opt.setContent(v);

            try {
                optionMapper.insert(opt);
            } catch (DuplicateKeyException e) {
                throw new IllegalArgumentException("OPTION_KEY_CONFLICT");
            }
        }

        return questionMapper.findById(q.getId());
    }

    @Override
    public Question getQuestion(long questionId) {
        Question q = questionMapper.findById(questionId);
        if (q == null) throw new IllegalArgumentException("QUESTION_NOT_FOUND");
        return q;
    }

    @Override
    public List<Question> listQuestionsByMaterial(long materialId) {
        return questionMapper.listByMaterialId(materialId);
    }

    @Override
    public List<QuestionOption> listOptions(long questionId) {
        // 外键存在性校验（避免查一个不存在 question 的 options）
        if (questionMapper.findById(questionId) == null) {
            throw new IllegalArgumentException("QUESTION_NOT_FOUND");
        }
        return optionMapper.listByQuestionId(questionId);
    }

    @Override
    @Transactional
    public Question updateQuestion(long questionId, String stem, BigDecimal difficulty,
                                   String correctKey, Map<String, String> optionsOrNull) {

        Question old = questionMapper.findById(questionId);
        if (old == null) throw new IllegalArgumentException("QUESTION_NOT_FOUND");

        // 不允许改 materialId / qOrder（保证绑定语义）
        Question q = new Question();
        q.setId(questionId);
        q.setStem(isBlank(stem) ? old.getStem() : stem);
        q.setDifficulty(difficulty == null ? old.getDifficulty() : difficulty);

        String newCorrect = isBlank(correctKey) ? old.getCorrectKey() : normKey(correctKey);

        // 如果要改 options：要求 correctKey 必须在 options 内
        if (optionsOrNull != null) {
            validateOptions(optionsOrNull, newCorrect);
            // 先清空再插入（简单可靠）
            optionMapper.deleteByQuestionId(questionId);
            for (Map.Entry<String, String> en : optionsOrNull.entrySet()) {
                QuestionOption opt = new QuestionOption();
                opt.setQuestionId(questionId);
                opt.setOptKey(normKey(en.getKey()));
                opt.setContent(en.getValue());
                optionMapper.insert(opt);
            }
        } else {
            // 不改 options：correctKey 仍然要保持合法（这里保守起见，不额外校验）
        }

        q.setCorrectKey(newCorrect);

        questionMapper.updateContent(q);
        return questionMapper.findById(questionId);
    }

    @Override
    @Transactional
    public void deleteQuestion(long questionId) {
        // ON DELETE CASCADE 会自动删 options
        int rows = questionMapper.deleteById(questionId);
        if (rows <= 0) throw new IllegalArgumentException("QUESTION_NOT_FOUND");
    }

    // ---------- helpers ----------

    private static void validateOptions(Map<String, String> options, String correctKey) {
        if (options.size() < 2) throw new IllegalArgumentException("OPTIONS_TOO_FEW");

        // key 去重/合法性
        Set<String> keys = new HashSet<>();
        for (Map.Entry<String, String> en : options.entrySet()) {
            String k = normKey(en.getKey());
            if (!isValidKey(k)) throw new IllegalArgumentException("INVALID_OPTION_KEY");
            if (isBlank(en.getValue())) throw new IllegalArgumentException("OPTION_CONTENT_EMPTY");
            if (!keys.add(k)) throw new IllegalArgumentException("DUPLICATE_OPTION_KEY");
        }

        if (!keys.contains(correctKey)) throw new IllegalArgumentException("CORRECT_KEY_NOT_IN_OPTIONS");
    }

    private static boolean isValidKey(String k) {
        return "A".equals(k) || "B".equals(k) || "C".equals(k) || "D".equals(k);
    }

    private static String normKey(String k) {
        return k == null ? "" : k.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
