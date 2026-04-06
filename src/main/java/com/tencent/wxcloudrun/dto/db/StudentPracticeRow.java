package com.tencent.wxcloudrun.dto.db;

import lombok.Data;

@Data
public class StudentPracticeRow {
    private Long studentId;
    private String studentNo;
    private String studentName;

    private Long materialId;
    private String materialTitle;

    private Long questionId;
    private Integer qOrder;

    private String correctKey;
    private String selectedKey;
    private Integer isCorrect; // MySQL tinyint -> Integer/Boolean 都行
}
