package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dao.MaterialMapper;
import com.tencent.wxcloudrun.dto.quest.GetPracticeRequest;
import com.tencent.wxcloudrun.dto.quest.MaterialDetailDTO;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.service.MaterialService;
import org.apache.ibatis.annotations.Delete;
import org.springframework.security.core.Authentication;
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
            // System.out.println(materialList);
            return ApiResponse.ok(materialList);
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-practice")
    public ApiResponse getAMaterial(Authentication authentication) {
        try {
            Long studentId = (Long) authentication.getPrincipal();
            GetPracticeRequest m = materialService.getAMaterial(studentId);
            return ApiResponse.ok(m);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-material/{id}")
    public ApiResponse getMaterialById(@PathVariable("id") Long materialId) {
        try {
            UploadMaterialRequest dto = materialService.getMaterialDtoById(materialId);
            return ApiResponse.ok(dto);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/activate/{id}")
    public ApiResponse activateMaterial(@PathVariable("id") Long id) {
        try {
            int out = materialService.activate(id);
            return ApiResponse.ok(Map.of("materialId", id, "activated", out));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/update/{id}")
    public ApiResponse updateMaterial(@PathVariable("id") Long materialId,
                                      @RequestBody UploadMaterialRequest req) {
        try {
            return ApiResponse.ok(materialService.updateMaterialWithQuestions(materialId, req));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/delete/{id}")
    public ApiResponse softDelete(@PathVariable("id") Long id) {
        try {
            materialService.softDelete(id);
            return ApiResponse.ok(Map.of("materialId", id, "deleted", true));
        } catch (IllegalArgumentException e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @DeleteMapping("/hard-delete/{id}")
    public ApiResponse hardDelete(@PathVariable("id") Long id) {
        try {
            materialService.hardDelete(id);
            return ApiResponse.ok(Map.of("materialId", id, "delete", true));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
