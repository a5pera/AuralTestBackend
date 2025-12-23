package com.tencent.wxcloudrun.model.auth;

import lombok.Data;

@Data
public class AvatarAsset {
    public Long studentId;
    public String sourceType;
    public String localPath;
    public String mimeType;
}
