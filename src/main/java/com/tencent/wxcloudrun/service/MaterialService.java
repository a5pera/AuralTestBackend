package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.quest.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface MaterialService {

    // 创建（通常管理员用）
    MaterialDetailDTO create(MaterialDetailDTO req);

    // 详情（通常管理员/内部用：带 is_active/时间等）
    MaterialDetailDTO getDetail(long materialId);

    // 列表（学生用：不一定返回 transcript；按 done 状态决定）
    List<MaterialItemDTO> listForStudent(long studentId, int page, int pageSize);

    // 单条（学生用：按 done 状态决定是否带 transcript）
    MaterialItemDTO getForStudent(long studentId, long materialId);

    // 更新（管理员用）
    MaterialDetailDTO update(long materialId, MaterialDetailDTO req);

    // 给学生推送一条合适难度的练习
    GetPracticeRequest getAMaterial(long studentId);

    // 软删除（管理员用：is_active=0）
    void softDelete(Long materialId);

    // 插入材料
    Map<String, Object> createMaterialWithQuestions(UploadMaterialRequest req);

    // 更新材料对应的音频
    int updateAudioIdByMaterialId(UpdateAudioIdDTO updateAudioIdDTO);

    // 删除材料
    int hardDelete(Long materialId);
}
