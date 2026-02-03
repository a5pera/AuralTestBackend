package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class RedoGetPracticeRequest {
    private Long materialId;
    private String materialTitle;
    private String materialTranscript;
    private Long audioId;
    private String audioPath;
    private String audioType;
    private BigDecimal materialLevel;

    private List<QuestionPracticeDTO> questions;

    @Data
    public static class QuestionPracticeDTO {
        private Long qId;
        private Integer qOrder;
        private String correctKey;
        private List<OptionDTO> options;
    }

    @Data
    public static class OptionDTO {
        private String optKey;
        private String content;
    }
}
