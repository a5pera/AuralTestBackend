package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClassOneQuestionAccuracyDTO {
    private Long classId;
    private Long questionId;
    private Long correctCnt;
    private Long attemptCnt;
    private BigDecimal accuracy; // 0~1
}
