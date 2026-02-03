package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dao.AttemptAnswerMapper;
import com.tencent.wxcloudrun.dao.AttemptMapper;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDTO;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDetailedDTO;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.dto.quest.PracticedQuestionCountDTO;
import com.tencent.wxcloudrun.dto.quest.RedoGetPracticeRequest;
import com.tencent.wxcloudrun.model.quest.Material;
import com.tencent.wxcloudrun.model.user.Attempt;
import com.tencent.wxcloudrun.model.user.AttemptAnswer;
import com.tencent.wxcloudrun.service.PracticeService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/practice")
public class PracticeController {
    private final PracticeService practiceService;

    private final AttemptAnswerMapper attemptAnswerMapper;

    private final AttemptMapper attemptMapper;

    public PracticeController(PracticeService practiceService,
                              AttemptAnswerMapper attemptAnswerMapper,
                              AttemptMapper attemptMapper) {
        this.practiceService = practiceService;
        this.attemptAnswerMapper = attemptAnswerMapper;
        this.attemptMapper = attemptMapper;
    }

    @PostMapping("/submit")
    public ApiResponse submit(@RequestBody PracticeSubmitRequest req, Authentication authentication) {
        try {
            Long studentId = (Long) authentication.getPrincipal();
            PracticeSubmitResponse out = practiceService.submitAndUpdate(studentId, req);
            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-practiced")
    public ApiResponse getPracticed(Authentication authentication) {
        try {
            Long studentId = (Long) authentication.getPrincipal();
            List<PracticeAttemptDTO> attemptsByStudentId = practiceService.getAttemptsByStudentId(studentId);
            return ApiResponse.ok(attemptsByStudentId);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-practiced-detail/{attemptId}")
    public ApiResponse getPracticedDetail(@PathVariable Long attemptId) {
        try {
            List<PracticeAttemptDetailedDTO> out = practiceService.getDetailByAttemptId(attemptId);
            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-recommend")
    public ApiResponse getRecommendPractice(Authentication authentication) {
        try {
            Long studentId = (Long) authentication.getPrincipal();
            List<Material> materials = practiceService.recommendTop5Materials(studentId);
            return ApiResponse.ok(materials);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/question-count")
    public ApiResponse getQuestionCount(Authentication authentication) {
        try {
            if (authentication == null || authentication.getPrincipal() == null) {
                return ApiResponse.error("NO_AUTH");
            }

            Long studentId = (Long) authentication.getPrincipal();

            long materialCount = attemptMapper.countDistinctMaterialByStudentId(studentId);
            long cnt = attemptAnswerMapper.countPracticedQuestions(studentId);
            long uniq = attemptAnswerMapper.countUniquePracticedQuestions(studentId);

            PracticedQuestionCountDTO out = new PracticedQuestionCountDTO();
            out.setMaterialCount(materialCount);
            out.setPracticedQuestionCount(cnt);
            out.setUniqueQuestionCount(uniq);

            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/redo/{materialId}")
    public ApiResponse redo(@PathVariable("materialId") Long materialId,
                            Authentication authentication) {
        try {
            Long studentId = (Long) authentication.getPrincipal(); // 你项目里就是这么取的
            RedoGetPracticeRequest dto = practiceService.getRedoMaterial(studentId, materialId);
            return ApiResponse.ok(dto);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
