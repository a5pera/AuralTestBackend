package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.AudioAssetMapper;
import com.tencent.wxcloudrun.dto.quest.RegisterAudioRequest;
import com.tencent.wxcloudrun.model.user.AudioAsset;
import com.tencent.wxcloudrun.service.AudioService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

@Service
public class AudioServiceImpl implements AudioService {

    @Resource
    private AudioAssetMapper audioAssetMapper;

    @Override
    @Transactional
    public AudioAsset registerUploadedAudio(RegisterAudioRequest req) {
        if (req == null) throw new IllegalArgumentException("MISSING_BODY");
        if (isBlank(req.getFileId())) throw new IllegalArgumentException("MISSING_FILE_ID");

        // 可选：sha256 去重（如果你希望同音频不重复入库）
        if (!isBlank(req.getSha256())) {
            AudioAsset existed = audioAssetMapper.findBySha256(req.getSha256().trim());
            if (existed != null) return existed;
        }

        AudioAsset a = new AudioAsset();
        a.setLocalPath(req.getFileId().trim());
        a.setMimeType(isBlank(req.getMimeType()) ? "audio/mpeg" : req.getMimeType().trim());
        a.setDurationMs(req.getDurationMs()); // 允许为空
        a.setBytes(req.getBytes());           // 允许为空
        a.setSha256(isBlank(req.getSha256()) ? null : req.getSha256().trim());

        audioAssetMapper.insert(a);

        // 你也可以直接 return a; 因为 id 已回填
        return audioAssetMapper.findById(a.getId());
    }

    @Override
    public AudioAsset findById(long id) {
        return null;
    }

    @Override
    public void deleteById(long id) {

    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}