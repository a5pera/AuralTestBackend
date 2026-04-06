package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class ClassMaterialAccuracyDTO {
    private Long materialId;
    private String materialTitle;
    private BigDecimal materialLevel;
    private BigDecimal accuracy;
}
