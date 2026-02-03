package com.tencent.wxcloudrun.dto.db;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class RedoMaterialRow {
    private Long materialId;
    private String materialTitle;
    private String materialTranscript;
    private BigDecimal materialLevel;
    private Long audioId;
    private String audioPath;
    private String audioType;
}
