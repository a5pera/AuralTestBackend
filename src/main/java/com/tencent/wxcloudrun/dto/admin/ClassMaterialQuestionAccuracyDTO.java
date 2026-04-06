package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClassMaterialQuestionAccuracyDTO {
    private Long materialId;
    private Long questionId;
    private Integer qOrder;

    private Long correctCnt;
    private Long studentCnt;
    private BigDecimal accuracy;
}
