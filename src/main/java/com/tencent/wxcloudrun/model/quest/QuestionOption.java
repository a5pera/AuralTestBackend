package com.tencent.wxcloudrun.model.quest;

import lombok.Data;

@Data
public class QuestionOption {
    private Long id;
    private Long questionId;
    private String optKey;   // "A"/"B"/"C"/"D"
    private String content;
}