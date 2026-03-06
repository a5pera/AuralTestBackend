package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ClassStudentDTO {
    private Long rosterId;

    private Long studentId;        // 已绑定才有
    private String accountStatus;  // "BOUND" / "UNBOUND"

    private String name;
    private String studentNo;
    private String college;

    private Long practicedQuestionCount; // 已绑定：统计；未绑定：0
    private BigDecimal theta;            // 已绑定：theta；未绑定：null
}
