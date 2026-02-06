package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecommendMaterialDTO {
    public Long id;
    public String title;
    public BigDecimal level;
    private Long audioId;
}
