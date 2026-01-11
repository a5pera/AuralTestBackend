package com.tencent.wxcloudrun.model.quest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Question {
    private Long id;
    private Long materialId;
    private Integer qOrder;
    private String stem;
    private BigDecimal difficulty;
    private String correctKey;     // "A"/"B"/"C"/"D"
    private LocalDateTime createdAt;
}