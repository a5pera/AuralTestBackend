package com.tencent.wxcloudrun.model.user;

import lombok.Data;

@Data
public class AttemptAnswer {
    private Long id;

    private Long attemptId;
    private Long questionId;

    private String chosenKey;   // A/B/C/D
    private Boolean isCorrect;  // tinyint(1)
}
