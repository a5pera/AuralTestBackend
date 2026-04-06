package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class StudentPracticeReportDTO {
    private Long studentId;
    private String studentNo;
    private String studentName;

    private List<MaterialPracticeDTO> materials;

    @Data
    public static class MaterialPracticeDTO {
        private Long materialId;
        private String materialTitle;

        private BigDecimal accuracy; // 0~1（该材料最新一次练习的正确率）
        private List<QuestionAnswerDTO> questions;
    }

    @Data
    public static class QuestionAnswerDTO {
        private Long questionId;
        private Integer qOrder;
        private String correctKey;     // 正确答案
        private String selectedKey;    // 学生所选
        private Boolean isCorrect;     // 是否正确
    }
}
