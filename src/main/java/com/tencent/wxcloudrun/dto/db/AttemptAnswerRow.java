package com.tencent.wxcloudrun.dto.db;

import lombok.Data;

@Data
public class AttemptAnswerRow {
    private Long questionId;
    private String chosenKey;
}
