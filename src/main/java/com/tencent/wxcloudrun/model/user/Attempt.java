package com.tencent.wxcloudrun.model.user;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Attempt {
    private Long id;

    private Long studentId;
    private Long materialId;

    private Integer totalQ;

    private BigDecimal thetaBefore;
    private BigDecimal thetaAfter;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
