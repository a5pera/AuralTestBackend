package com.tencent.wxcloudrun.dto.practice;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PracticeAttemptDTO {
    private Long attemptId;
    private Long materialId;
    private String materialTitle;
    private BigDecimal thetaBefore;
    private BigDecimal thetaAfter;
    private LocalDateTime submittedAt;
}
