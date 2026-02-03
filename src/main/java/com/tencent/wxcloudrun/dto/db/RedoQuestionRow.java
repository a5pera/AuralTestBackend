package com.tencent.wxcloudrun.dto.db;

import lombok.Data;

@Data
public class RedoQuestionRow {
    private Long qId;
    private Integer qOrder;
    private String correctKey;
}
