package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.model.user.Attempt;
import com.tencent.wxcloudrun.model.user.AttemptAnswer;

import java.util.List;

public interface PracticeService {
    PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req);

    List<Attempt> getAttemptsByStudentId(Long studentId);

    List<AttemptAnswer> getDetailByAttemptId(Long  attemptId);
}
