package com.tencent.wxcloudrun.dto.db;

import lombok.Data;

@Data
public class QuestionOptionRow {
    private Long questionId;
    private String optKey;
    private String content;
}
