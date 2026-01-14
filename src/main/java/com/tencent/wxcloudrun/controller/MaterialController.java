package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dao.MaterialMapper;
import com.tencent.wxcloudrun.dto.quest.MaterialDetailDTO;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.service.MaterialService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/material")
public class MaterialController {
    private final MaterialService materialService;
    private final MaterialMapper materialMapper;
    public MaterialController(MaterialService materialService, MaterialMapper materialMapper) {
        this.materialService = materialService;
        this.materialMapper = materialMapper;
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

    @GetMapping("/list")
    public ApiResponse list() {
        try {
            List<Material> materialList = materialMapper.listAll();
            System.out.println(materialList);
            return ApiResponse.ok(materialList);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
