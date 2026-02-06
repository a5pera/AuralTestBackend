package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class UploadMaterialRequest {
    // ------- material -------
    private String title;
    private BigDecimal level;      // 可选：材料难度
    private String transcript;
    private Boolean isActive;
    private Long audioId;

    // ------- questions -------
    private List<QuestionCreateDTO> questions;

    public UploadMaterialRequest() {
    }

    @Data
    public static class QuestionCreateDTO {
        private Integer qOrder;          // 题号/顺序
        private String stem;             // 题干
        private BigDecimal difficulty;   // 题目难度
        private String correctKey;       // A/B/C/D
        private List<OptionDTO> options; // 选项
    }

    @Data
    public static class OptionDTO {
        private String optKey;   // A/B/C/D
        private String content;
    }
}