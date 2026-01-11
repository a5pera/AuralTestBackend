package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data
public class MaterialDetailDTO {
    private Long id;
    private String title;
    private BigDecimal level;
    private String transcript;
    private Long audioId;
    private Boolean is_active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
