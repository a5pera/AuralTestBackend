package com.tencent.wxcloudrun.dto.practice;

import lombok.Data;

@Data
public class PracticeAttemptDetailedDTO {
    private Long id;
    private Long attemptId;
    private Long questionId;
    private String chosenKey;
    private String correctKey;
    private Boolean isCorrect;
}
