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

@RestController
@RequestMapping("api/material")
public class MaterialController {
//    private final MaterialService materialService;
//
//    public MaterialController(MaterialService materialService) {
//        this.materialService = materialService;
//    }
//    @PostMapping("/uploadMaterial")
//    public ApiResponse uploadMaterial(@RequestBody UploadMaterialRequest req,
//                                      HttpServletRequest request) {
//        // 调 service：事务创建 material + questions + options
//        return ApiResponse.ok(materialService.createMaterialWithQuestions(req, request));
//    }
}
