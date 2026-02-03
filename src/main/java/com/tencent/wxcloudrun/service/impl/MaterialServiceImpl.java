package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.*;
import com.tencent.wxcloudrun.dto.db.QuestionOptionRow;
import com.tencent.wxcloudrun.dto.quest.*;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.quest.QuestionOption;
import com.tencent.wxcloudrun.model.user.AudioAsset;
import com.tencent.wxcloudrun.service.MaterialService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
public class MaterialServiceImpl implements MaterialService {

    @Resource
    private MaterialMapper materialMapper;
    @Resource
    private QuestionMapper questionMapper;
    @Resource
    private QuestionOptionMapper questionOptionMapper;
    @Resource
    private AudioAssetMapper audioAssetMapper;
    @Resource
    private StudentMapper studentMapper;

    @Override
    public MaterialDetailDTO create(MaterialDetailDTO req) {
        return null;
    }

    @Override
    public MaterialDetailDTO getDetail(long materialId) {
        return null;
    }

    @Override
    public List<MaterialItemDTO> listForStudent(long studentId, int page, int pageSize) {
        return null;
    }

    @Override
    public MaterialItemDTO getForStudent(long studentId, long materialId) {
        return null;
    }

    @Override
    @Transactional
    public Map<String, Object> updateMaterialWithQuestions(Long materialId, UploadMaterialRequest req) {
        if (materialId == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (isBlank(req.getTitle()) || isBlank(req.getTranscript()) || req.getAudioId() == null) {
            throw new IllegalArgumentException("MISSING_PARAMS");
        }
        if (req.getQuestions() == null || req.getQuestions().isEmpty()) {
            throw new IllegalArgumentException("QUESTIONS_EMPTY");
        }

        Material old = materialMapper.findById(materialId);
        if (old == null) throw new IllegalArgumentException("MATERIAL_NOT_FOUND");

        // 1) 计算/确定 level（若未传则用题目 difficulty 平均）
        BigDecimal level = req.getLevel();
        if (level == null) {
            level = computeLevelFromQuestions(req.getQuestions()).orElse(
                    old.getLevel() != null ? old.getLevel() : new BigDecimal("5.00")
            );
        }

        // 2) 更新 materials
        Material m = new Material();
        m.setId(materialId);
        m.setTitle(req.getTitle().trim());
        m.setTranscript(req.getTranscript());
        m.setAudioId(req.getAudioId());
        m.setLevel(level);
        materialMapper.updateById(m);

        // 3) 取出该 material 现有 questions（一次性），用 qOrder 做映射
        List<Question> existedQs = questionMapper.listByMaterialId(materialId);
        Map<Integer, Question> qByOrder = new HashMap<>();
        if (existedQs != null) {
            for (Question q : existedQs) {
                if (q != null && q.getQOrder() != null) qByOrder.put(q.getQOrder(), q);
            }
        }

        int insertedQ = 0, updatedQ = 0;
        int optionUpsert = 0, optionDeleted = 0;

        // 4) 逐题更新/插入 + 同步选项
        Set<Integer> orderSet = new HashSet<>();
        for (UploadMaterialRequest.QuestionCreateDTO qdto : req.getQuestions()) {
            if (qdto == null) throw new IllegalArgumentException("QUESTION_EMPTY");
            if (qdto.getQOrder() == null) throw new IllegalArgumentException("QUESTION_ORDER_MISSING");
            if (!orderSet.add(qdto.getQOrder())) throw new IllegalArgumentException("QUESTION_ORDER_DUPLICATE");
            if (isBlank(qdto.getStem())) throw new IllegalArgumentException("QUESTION_STEM_MISSING");
            if (qdto.getDifficulty() == null) throw new IllegalArgumentException("QUESTION_DIFFICULTY_MISSING");
            if (isBlank(qdto.getCorrectKey())) throw new IllegalArgumentException("QUESTION_CORRECT_KEY_MISSING");
            if (qdto.getOptions() == null || qdto.getOptions().isEmpty()) throw new IllegalArgumentException("OPTIONS_EMPTY");

            String correctKey = normKey(qdto.getCorrectKey());

            Question exist = qByOrder.get(qdto.getQOrder());
            Long questionId;

            if (exist == null) {
                // insert
                Question nq = new Question();
                nq.setMaterialId(materialId);
                nq.setQOrder(qdto.getQOrder());
                nq.setStem(qdto.getStem().trim());
                nq.setDifficulty(qdto.getDifficulty());
                nq.setCorrectKey(correctKey);
                questionMapper.insert(nq); // useGeneratedKeys 回填 id
                questionId = nq.getId();
                insertedQ++;
            } else {
                // update
                questionMapper.updateCoreById(exist.getId(), qdto.getStem().trim(), qdto.getDifficulty(), correctKey);
                questionId = exist.getId();
                updatedQ++;
            }

            if (questionId == null) throw new IllegalStateException("QUESTION_ID_MISSING_AFTER_SAVE");

            // 选项：upsert + 删除不在 DTO 的旧选项
            Set<String> optKeys = new HashSet<>();
            for (UploadMaterialRequest.OptionDTO odto : qdto.getOptions()) {
                if (odto == null) throw new IllegalArgumentException("OPTION_EMPTY");
                if (isBlank(odto.getOptKey()) || isBlank(odto.getContent())) {
                    throw new IllegalArgumentException("OPTION_PARAM_MISSING");
                }
                String k = normKey(odto.getOptKey());
                if (!optKeys.add(k)) throw new IllegalArgumentException("OPTION_KEY_DUPLICATE");

                optionUpsert += questionOptionMapper.upsert(questionId, k, odto.getContent().trim());
            }

            // 删除本题下“不在本次请求里的旧 opt_key”
            optionDeleted += questionOptionMapper.deleteNotInKeys(questionId, new ArrayList<>(optKeys));
        }

        Map<String, Object> out = new HashMap<>();
        out.put("materialId", materialId);
        out.put("level", level);
        out.put("insertedQuestions", insertedQ);
        out.put("updatedQuestions", updatedQ);
        out.put("optionUpsertAffectedRows", optionUpsert);
        out.put("deletedOptions", optionDeleted);
        return out;
    }

    // 这里的level是学生的level
    public GetPracticeRequest getAMaterial(long studentId) {
        // if (level == null) throw new IllegalArgumentException("LEVEL_MISSING");

        List<MaterialIdAndLevel> candidates = materialMapper.listIdAndLevel(studentId);
        Student s = studentMapper.findById(String.valueOf(studentId));
        BigDecimal level = s.getTheta();
        Random r = new Random();
        if (candidates == null || candidates.isEmpty()) {
            throw new IllegalArgumentException("NO_MATERIAL");
        }

        // 1) 采样 d_target ~ TruncNormal(mu=level, sigma), truncated to [mu-1, mu+1]
        double mu = level.doubleValue();
        double min = mu - 1.0;
        double max = mu + 1.0;
        final double EPS = 1e-12;

        // sigma 选小一点，保证大部分样本都落在 ±1 内，拒绝采样几乎不会循环多次
        double sigma = 0.4; // 经验值：±1 约等于 ±2.5σ，接受率很高
        double dTarget = sampleTruncatedNormal(mu, sigma, min, max);

        // 2) 找 level 最接近 dTarget 的 material
        MaterialIdAndLevel best = null;
        double bestDiff = Double.POSITIVE_INFINITY;

        for (MaterialIdAndLevel it : candidates) {
            if (it == null || it.getMaterialId() == null || it.getLevel() == null) continue;
            double diff = Math.abs(it.getLevel().doubleValue() - dTarget);
            if (diff + EPS < bestDiff) {
                bestDiff = diff;
                best = it;
            } else if (Math.abs(diff - bestDiff) <= EPS && best != null) {
                // 可选：差值一样时用 id 小的（或随机）
                // 随机选择
                if (r.nextInt(10) < 5) best = it;
                // if (it.getMaterialId() < best.getMaterialId()) best = it;
            }
        }

        if (best == null) throw new IllegalArgumentException("NO_VALID_MATERIAL");

        // 3) 查详情并返回（你说的 mapper.findById）
        Material m = materialMapper.findById(best.getMaterialId());
        if (m == null) throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        AudioAsset audio = audioAssetMapper.findById(m.getAudioId());

        GetPracticeRequest res = new GetPracticeRequest();
        List<Question> questions = questionMapper.listByMaterialId(m.getId());
        res.setMaterialId(m.getId());
        res.setMaterialLevel(m.getLevel());
        res.setAudioId(m.getAudioId());
        res.setMaterialTitle(m.getTitle());
        res.setAudioPath(audio.getLocalPath());
        res.setAudioType(audio.getMimeType());
        List<GetPracticeRequest.QuestionPracticeDTO> questionPracticeDTOList = new ArrayList<>();
        for (Question question : questions) {
            List<QuestionOption> questionOptions = questionOptionMapper.listByQuestionId(question.getId());
            GetPracticeRequest.QuestionPracticeDTO questionPracticeDTO = new GetPracticeRequest.QuestionPracticeDTO();
            questionPracticeDTO.setQOrder(question.getQOrder());
            questionPracticeDTO.setQId(question.getId());
            // questionPracticeDTO.setCorrectKey(question.getCorrectKey());
            List<GetPracticeRequest.OptionDTO> optionDTOList = new ArrayList<>();
            for (QuestionOption questionOption : questionOptions) {
                GetPracticeRequest.OptionDTO optionDTO = new GetPracticeRequest.OptionDTO();
                optionDTO.setContent(questionOption.getContent());
                optionDTO.setOptKey(questionOption.getOptKey());
                optionDTOList.add(optionDTO);
            }
            questionPracticeDTO.setOptions(optionDTOList);
            questionPracticeDTOList.add(questionPracticeDTO);
        }
        res.setQuestions(questionPracticeDTOList);
        return res;
    }

    @Override
    @Transactional
    public void softDelete(Long materialId) {
        if (materialId == null) throw new IllegalArgumentException("MISSING_MATERIAL_ID");

        // 可选：先判断存在，给更明确的错误码
        if (materialMapper.findById(materialId) == null) {
            throw new IllegalArgumentException("MATERIAL_NOT_FOUND");
        }

        int affected = materialMapper.softDelete(materialId);
        if (affected == 0) {
            // 已经被删除（is_active=0）或并发导致没更新
            throw new IllegalArgumentException("MATERIAL_ALREADY_INACTIVE");
        }
    }

    @Override
    public int activate(Long materialId) {
        if (materialId == null) throw new IllegalArgumentException("MISSING_MATERIAL_ID");
        return materialMapper.active(materialId);
    }

    @Override
    public int updateAudioIdByMaterialId(UpdateAudioIdDTO req) {
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        return materialMapper.updateAudioIdByMaterialId(req.getAudioId(), req.getMaterialId());
    }

    @Override
    public int hardDelete(Long materialId) {
        List<Question> questions = questionMapper.listByMaterialId(materialId);
        for (Question question : questions) {
            questionOptionMapper.deleteByQuestionId(question.getId());
            questionMapper.deleteById(question.getId());
        }
        return materialMapper.deleteHard(materialId);
    }

    @Override
    @Transactional
    public Map<String, Object> createMaterialWithQuestions(UploadMaterialRequest req) {
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (isBlank(req.getTitle()) || isBlank(req.getTranscript()) || req.getAudioId() == null) {
            throw new IllegalArgumentException("MISSING_PARAMS");
        }

        // 1) 外键校验：audio_id 必须存在（materials.audio_id -> audio_assets.id RESTRICT）
//        if (!audioAssetMapper.existsById(req.getAudioId())) {
//            throw new IllegalArgumentException("AUDIO_NOT_FOUND");
//        }

        // 2) 如果未传 level：可用题目 difficulty 均值作为缓存字段（文档建议 level 缓存题目均值）
        BigDecimal level = req.getLevel();
        if (level == null) {
            level = computeLevelFromQuestions(req.getQuestions()).orElse(new BigDecimal("5.00"));
        }

        // 3) 先插入 materials（拿到 materialId）
        Material m = new Material();
        m.setTitle(req.getTitle().trim());
        m.setTranscript(req.getTranscript());
        m.setAudioId(req.getAudioId());
        m.setLevel(level);
        m.setIsActive(true);

        materialMapper.insert(m); // useGeneratedKeys 回填 id
        long materialId = m.getId();

        int questionCount = 0;

        // 4) 再插入 questions & options（同一事务）
        List<UploadMaterialRequest.QuestionCreateDTO> qs = req.getQuestions();
        if (qs != null && !qs.isEmpty()) {
            // 请求内 qOrder 去重（避免 DB uk(material_id,q_order) 冲突才发现）
            Set<Integer> orderSet = new HashSet<>();

            for (UploadMaterialRequest.QuestionCreateDTO qdto : qs) {
                if (qdto == null) throw new IllegalArgumentException("QUESTION_EMPTY");
                if (qdto.getQOrder() == null) throw new IllegalArgumentException("QUESTION_ORDER_MISSING");
                if (!orderSet.add(qdto.getQOrder())) throw new IllegalArgumentException("QUESTION_ORDER_DUPLICATE");

                validateQuestionDTO(qdto);

                Question q = new Question();
                q.setMaterialId(materialId);
                q.setQOrder(qdto.getQOrder());
                q.setStem(qdto.getStem().trim());
                q.setDifficulty(qdto.getDifficulty());
                q.setCorrectKey(normKey(qdto.getCorrectKey()));

                try {
                    questionMapper.insert(q); // 回填 questionId
                } catch (DuplicateKeyException e) {
                    // uk_question_material_order(material_id,q_order)
                    throw new IllegalArgumentException("QUESTION_ORDER_CONFLICT");
                }

                long questionId = q.getId();

                for (UploadMaterialRequest.OptionDTO odto : qdto.getOptions()) {
                    QuestionOption opt = new QuestionOption();
                    opt.setQuestionId(questionId);
                    opt.setOptKey(normKey(odto.getOptKey()));
                    opt.setContent(odto.getContent().trim());

                    try {
                        questionOptionMapper.insert(opt);
                    } catch (DuplicateKeyException e) {
                        // uk_option_question_key(question_id,opt_key)
                        throw new IllegalArgumentException("OPTION_KEY_CONFLICT");
                    }
                }

                questionCount++;
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("materialId", materialId);
        out.put("questionCount", questionCount);
        return out;
    }

    @Override
    public UploadMaterialRequest getMaterialDtoById(Long materialId) {
        if (materialId == null) throw new IllegalArgumentException("MATERIAL_ID_MISSING");

        Material m = materialMapper.findById(materialId);
        if (m == null) throw new IllegalArgumentException("MATERIAL_NOT_FOUND");

        List<Question> qs = questionMapper.listByMaterialId(materialId);
        if (qs == null) qs = List.of();

        // 批量查 options：questionId -> options
        Map<Long, List<QuestionOption>> optMap = new HashMap<>();
        if (!qs.isEmpty()) {
            List<Long> qIds = qs.stream().map(Question::getId).toList();
            List<QuestionOptionRow> allOpts = questionOptionMapper.listByQuestionIds(qIds);

            for (QuestionOptionRow row : allOpts) {
                optMap.computeIfAbsent(row.getQuestionId(), k -> new ArrayList<>())
                        .add(toEntity(row));
            }

            // 可选：每题的选项按 optKey 排序 A/B/C/D
            for (List<QuestionOption> list : optMap.values()) {
                list.sort(Comparator.comparing(QuestionOption::getOptKey));
            }
        }

        // 组装 DTO
        UploadMaterialRequest out = new UploadMaterialRequest();
        out.setTitle(m.getTitle());
        out.setLevel(m.getLevel());
        out.setTranscript(m.getTranscript());
        out.setAudioId(m.getAudioId());

        // 题目按 qOrder 排序
        qs.sort(Comparator.comparing(Question::getQOrder, Comparator.nullsLast(Integer::compareTo)));

        List<UploadMaterialRequest.QuestionCreateDTO> qDtos = new ArrayList<>(qs.size());
        for (Question q : qs) {
            UploadMaterialRequest.QuestionCreateDTO qdto = new UploadMaterialRequest.QuestionCreateDTO();
            qdto.setQOrder(q.getQOrder());
            qdto.setStem(q.getStem());
            qdto.setDifficulty(q.getDifficulty());
            qdto.setCorrectKey(q.getCorrectKey());

            List<QuestionOption> opts = optMap.getOrDefault(q.getId(), List.of());
            List<UploadMaterialRequest.OptionDTO> odtos = new ArrayList<>(opts.size());
            for (QuestionOption o : opts) {
                UploadMaterialRequest.OptionDTO odto = new UploadMaterialRequest.OptionDTO();
                odto.setOptKey(o.getOptKey());
                odto.setContent(o.getContent());
                odtos.add(odto);
            }
            qdto.setOptions(odtos);
            qDtos.add(qdto);
        }

        out.setQuestions(qDtos);
        return out;
    }

    private static QuestionOption toEntity(QuestionOptionRow row) {
        QuestionOption o = new QuestionOption();
        o.setQuestionId(row.getQuestionId());
        o.setOptKey(row.getOptKey());
        o.setContent(row.getContent());
        return o;
    }

    // ---------------- helpers ----------------

    private static double sampleTruncatedNormal(double mu, double sigma, double min, double max) {
        // 拒绝采样：直到落入区间
        // Random.nextGaussian() 就是 N(0,1)
        var rnd = java.util.concurrent.ThreadLocalRandom.current();
        double x;
        int guard = 0;
        do {
            x = mu + sigma * rnd.nextGaussian();
            guard++;
            // 极端情况下防死循环：直接 clamp
            if (guard > 50) {
                if (x < min) return min;
                if (x > max) return max;
                return x;
            }
        } while (x < min || x > max);
        return x;
    }

    // 示例：你按自己 DTO 字段填
//    private MaterialDetailDTO toDetailDTO(Material m, double dTarget, double diff) {
//        MaterialDetailDTO dto = new MaterialDetailDTO();
//        dto.setMaterial(m);
//        dto.setDTarget(new BigDecimal(String.format(java.util.Locale.ROOT, "%.2f", dTarget)));
//        dto.setDiff(new BigDecimal(String.format(java.util.Locale.ROOT, "%.2f", diff)));
//        return dto;
//    }

    private static Optional<BigDecimal> computeLevelFromQuestions(List<UploadMaterialRequest.QuestionCreateDTO> qs) {
        if (qs == null || qs.isEmpty()) return Optional.empty();
        BigDecimal sum = BigDecimal.ZERO;
        int cnt = 0;
        for (UploadMaterialRequest.QuestionCreateDTO q : qs) {
            if (q != null && q.getDifficulty() != null) {
                sum = sum.add(q.getDifficulty());
                cnt++;
            }
        }
        if (cnt == 0) return Optional.empty();
        System.out.println("cnt=" + cnt);
        System.out.println("sum=" + sum);
        return Optional.of(sum.divide(BigDecimal.valueOf(cnt), 2, RoundingMode.HALF_UP));
    }

    private static void validateQuestionDTO(UploadMaterialRequest.QuestionCreateDTO q) {
        if (isBlank(q.getStem()) || q.getDifficulty() == null || isBlank(q.getCorrectKey())) {
            throw new IllegalArgumentException("QUESTION_MISSING_PARAMS");
        }
        if (q.getOptions() == null || q.getOptions().size() < 2) {
            throw new IllegalArgumentException("OPTIONS_TOO_FEW");
        }

        String correct = normKey(q.getCorrectKey());
        if (!isValidKey(correct)) throw new IllegalArgumentException("INVALID_CORRECT_KEY");

        Set<String> keys = new HashSet<>();
        for (UploadMaterialRequest.OptionDTO odto : q.getOptions()) {
            if (odto == null) throw new IllegalArgumentException("OPTION_EMPTY");
            String k = normKey(odto.getOptKey());
            if (!isValidKey(k)) throw new IllegalArgumentException("INVALID_OPTION_KEY");
            if (isBlank(odto.getContent())) throw new IllegalArgumentException("OPTION_CONTENT_EMPTY");
            if (!keys.add(k)) throw new IllegalArgumentException("DUPLICATE_OPTION_KEY");
        }

        if (!keys.contains(correct)) throw new IllegalArgumentException("CORRECT_KEY_NOT_IN_OPTIONS");
    }

    private static String normKey(String k) {
        return k == null ? "" : k.trim().toUpperCase(Locale.ROOT);
    }

    private static boolean isValidKey(String k) {
        return "A".equals(k) || "B".equals(k) || "C".equals(k) || "D".equals(k);
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
