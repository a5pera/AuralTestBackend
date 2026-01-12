package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.service.MaterialService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@RestController
@RequestMapping("api/material")
public class MaterialController {
    private MaterialService materialService;
    public MaterialController(MaterialService materialService) {
        this.materialService = materialService;
    }

    @PostMapping("/upload")
    public ApiResponse upload(@RequestBody UploadMaterialRequest req) {
        try {
            Map<String, Object> out = materialService.createMaterialWithQuestions(req);
            return ApiResponse.ok(out);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
