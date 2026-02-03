package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDTO;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDetailedDTO;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.dto.quest.RedoGetPracticeRequest;
import com.tencent.wxcloudrun.model.quest.Material;

import java.util.List;

public interface PracticeService {
    PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req);

    List<PracticeAttemptDTO> getAttemptsByStudentId(Long studentId);

    List<PracticeAttemptDetailedDTO> getDetailByAttemptId(Long attemptId);

    List<Material> recommendTop5Materials(long studentId);

    RedoGetPracticeRequest getRedoMaterial(Long studentId, Long materialId);
}