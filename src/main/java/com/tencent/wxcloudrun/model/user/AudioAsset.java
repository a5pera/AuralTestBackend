package com.tencent.wxcloudrun.model.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AudioAsset {
    private Long id;
    private String localPath;     // fileId
    private String mimeType;      // audio/mpeg
    private Integer durationMs;   // 可为空
    private Long bytes;           // 可为空
    private String sha256;        // 可为空
    private LocalDateTime createdAt;
}
