package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.admin.ClassMaterialAccuracyDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentCountDTO;
import com.tencent.wxcloudrun.dto.admin.ClassStudentDTO;
import com.tencent.wxcloudrun.dto.quest.UpdateClassNameDTO;
import com.tencent.wxcloudrun.dto.quest.UploadMaterialRequest;
import com.tencent.wxcloudrun.service.ClassesService;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
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

    @PostMapping("/update/{id}")
    public ApiResponse updateClassName(@PathVariable("id") Long id, @RequestBody UpdateClassNameDTO dto) {
        try {
            int out = classesService.updateClassName(id, dto.getNewClassName());
            return ApiResponse.ok(out);
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

    @GetMapping("/{classId}/students")
    public ApiResponse listStudents(@PathVariable Long classId,
                                    @RequestParam(required = false) Integer limit,
                                    @RequestParam(required = false) Integer offset) {
        List<ClassStudentDTO> list = classesService.listStudents(classId, limit, offset);
        return ApiResponse.ok(list);
    }

    @GetMapping("/{classId}/student-count")
    public ApiResponse countStudents(@PathVariable Long classId) {
        try {
            ClassStudentCountDTO dto = classesService.countStudents(classId);
            return ApiResponse.ok(dto);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/{classId}/material-accuracy")
    public ApiResponse getClassMaterialAccuracy(@PathVariable("classId") Long classId) {
        try {
            List<ClassMaterialAccuracyDTO> out = classesService.getMaterialAccuracy(classId);
            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
