package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.*;
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
    public MaterialDetailDTO update(long materialId, MaterialDetailDTO req) {
        return null;
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
    public int updateAudioIdByMaterialId(UpdateAudioIdDTO req) {
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        return materialMapper.updateAudioIdByMaterialId(req.getAudioId(), req.getMaterialId());
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
