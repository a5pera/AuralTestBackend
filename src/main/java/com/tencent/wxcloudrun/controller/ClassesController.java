package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.service.ClassesService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/class")
public class ClassesController {
    private final ClassesService classesService;

    public ClassesController(ClassesService classesService) {
        this.classesService = classesService;
    }

    @GetMapping("/list-all")
    public ApiResponse listAllClasses() {
        try {
            return ApiResponse.ok(classesService.listAllClasses());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/list")
    public ApiResponse listClasses() {
        try {
            return ApiResponse.ok(classesService.listActiveClasses());
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/activate/{id}")
    public ApiResponse activateClass(@PathVariable("id") Long id) {
        try {
            int out = classesService.activate(id);
            return ApiResponse.ok(Map.of("classId", id, "activated", out));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @PutMapping("/deActivate/{id}")
    public ApiResponse deActivateClass(@PathVariable("id") Long id) {
        try {
            int out = classesService.deActive(id);
            return ApiResponse.ok(Map.of("classId", id, "deActivated", out));
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
