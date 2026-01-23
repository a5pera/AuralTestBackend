package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dao.AttemptAnswerMapper;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitRequest;
import com.tencent.wxcloudrun.dto.quest.PracticeSubmitResponse;
import com.tencent.wxcloudrun.dto.quest.PracticedQuestionCountDTO;
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

    public PracticeController(PracticeService practiceService,
                              AttemptAnswerMapper attemptAnswerMapper) {
        this.practiceService = practiceService;
        this.attemptAnswerMapper = attemptAnswerMapper;
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
            List<Attempt> attemptsByStudentId = practiceService.getAttemptsByStudentId(studentId);
            return ApiResponse.ok(attemptsByStudentId);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-practiced-detail/{attemptId}")
    public ApiResponse getPracticedDetail(@PathVariable Long attemptId) {
        try {
            List<AttemptAnswer> out = practiceService.getDetailByAttemptId(attemptId);
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
        if (authentication == null || authentication.getPrincipal() == null) {
            return ApiResponse.error("NO_AUTH");
        }

        Long studentId = (Long) authentication.getPrincipal();

        long cnt = attemptAnswerMapper.countPracticedQuestions(studentId);
        long uniq = attemptAnswerMapper.countUniquePracticedQuestions(studentId);

        PracticedQuestionCountDTO out = new PracticedQuestionCountDTO();
        out.setPracticedQuestionCount(cnt);
        out.setUniqueQuestionCount(uniq);

        return ApiResponse.ok(out);
    }
}
