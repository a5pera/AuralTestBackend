package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.quest.RegisterAudioRequest;
import com.tencent.wxcloudrun.model.user.AudioAsset;
import com.tencent.wxcloudrun.service.AudioService;
import com.tencent.wxcloudrun.service.MaterialService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/audio")
public class AudioController {
    @Resource
    private AudioService audioService;
    @Resource
    private MaterialService materialService;

    @PostMapping("/register")
    public ApiResponse register(@RequestBody RegisterAudioRequest req) {
        try {
            AudioAsset a = audioService.registerUploadedAudio(req);
            // 返回给前端 audioId，后续创建 materials 时传 audioId
            return ApiResponse.ok(Map.of(
                    "audioId", a.getId(),
                    "fileId", a.getLocalPath(),
                    "mimeType", a.getMimeType(),
                    "durationMs", a.getDurationMs(),
                    "bytes", a.getBytes(),
                    "sha256", a.getSha256()
            ));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }


}
