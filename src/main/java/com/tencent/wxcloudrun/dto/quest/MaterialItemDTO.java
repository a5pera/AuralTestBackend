package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.math.BigDecimal;
@Data
public class MaterialItemDTO {
    private Long id;
    private String title;
    private BigDecimal level;
    private Long audioId;
    private String transcript;  // 这里只有判断学生做过的听力才给前端返回，否则不返回
}
