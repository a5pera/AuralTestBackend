package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

@Data
public class PracticedQuestionCountDTO {
    private long practicedQuestionCount;     // 累计练习题数（含重复）
    private long uniqueQuestionCount;        // 可选：去重题数
}
