package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.quest.RegisterAudioRequest;
import com.tencent.wxcloudrun.model.user.AudioAsset;

public interface AudioService {
    /**
     * 前端已上传文件到云存储后，登记到数据库 audio_assets
     *
     * @return 插入后的 AudioAsset（含 id）
     */
    AudioAsset registerUploadedAudio(RegisterAudioRequest req);
}
