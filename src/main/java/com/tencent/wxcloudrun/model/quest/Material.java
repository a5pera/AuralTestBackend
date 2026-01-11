package com.tencent.wxcloudrun.model.quest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Material {
    public Long id;
    public String title;
    private BigDecimal level;
    private String transcript;  // 原文

    private Long audioId;  // 听力音频id外键

    private Boolean isActive;   // 当前是否有效

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
