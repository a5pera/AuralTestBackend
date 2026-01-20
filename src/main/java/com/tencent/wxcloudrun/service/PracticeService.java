package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;

public interface PracticeService {
    PracticeSubmitResponse submitAndUpdate(Long studentId, PracticeSubmitRequest req);
}
