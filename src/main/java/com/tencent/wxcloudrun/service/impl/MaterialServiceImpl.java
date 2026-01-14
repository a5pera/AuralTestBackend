package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.MaterialMapper;
import com.tencent.wxcloudrun.dao.QuestionMapper;
import com.tencent.wxcloudrun.dao.QuestionOptionMapper;
import com.tencent.wxcloudrun.dto.quest.MaterialDetailDTO;
import com.tencent.wxcloudrun.dto.quest.MaterialItemDTO;
import com.tencent.wxcloudrun.dto.quest.UpdateAudioIdDTO;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.model.quest.Question;
import com.tencent.wxcloudrun.model.quest.QuestionOption;
import com.tencent.wxcloudrun.dao.AudioAssetMapper;
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

    @Resource private MaterialMapper materialMapper;
    @Resource private QuestionMapper questionMapper;
    @Resource private QuestionOptionMapper questionOptionMapper;
    @Resource
    private AudioAssetMapper audioAssetMapper;

    @Override
    public MaterialDetailDTO create(MaterialDetailDTO req) {
        return null;
    }

    @Override
    public List<MaterialDetailDTO> listAllMaterials() {
        List<Material> materials = materialMapper.listAll();
        List<MaterialDetailDTO> materialDetailDTOS = new ArrayList<>();
        for (Material material : materials) {
            MaterialDetailDTO temp = new MaterialDetailDTO();
            temp.setId(material.getId());
            temp.setTitle(material.getTitle());
            temp.setLevel(material.getLevel());
            temp.setTranscript(material.getTranscript());
            temp.setAudioId(material.getAudioId());
            temp.setIs_active(material.getIsActive());
            temp.setCreatedAt(material.getCreatedAt());
            temp.setUpdatedAt(material.getUpdatedAt());
        }
        return materialDetailDTOS;
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

    @Override
    public void delete(long materialId) {

    }

    @Override
    public int updateAudioIdByMaterialId(UpdateAudioIdDTO req) {
        if(req == null) throw new IllegalArgumentException("MISSING_BODY");
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
