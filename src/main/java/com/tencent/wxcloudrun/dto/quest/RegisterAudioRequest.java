package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

@Data
public class RegisterAudioRequest {
    private String fileId;     // 云存储 fileID
    private String mimeType;
    private Integer durationMs;
    private Long bytes;
    private String sha256;
}
