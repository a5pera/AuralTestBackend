package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.admin.LeaderboardResponse;
import com.tencent.wxcloudrun.dto.admin.StudentPracticeReportDTO;
import com.tencent.wxcloudrun.dto.auth.AdminLoginRequest;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDTO;
import com.tencent.wxcloudrun.dto.practice.PracticeAttemptDetailedDTO;
import com.tencent.wxcloudrun.model.user.AttemptAnswer;
import com.tencent.wxcloudrun.service.*;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final MaterialService materialService;
    private final StatisticsService statisticsService;
    private final PracticeService practiceService;
    private final AdminPracticeService adminPracticeService;

    public AdminController(AuthService authService, MaterialService materialService,
                           StatisticsService statisticsService,
                           PracticeService practiceService,
                           AdminPracticeService adminPracticeService) {
        this.authService = authService;
        this.materialService = materialService;
        this.statisticsService = statisticsService;
        this.practiceService = practiceService;
        this.adminPracticeService = adminPracticeService;
    }

    @PostMapping("/create")
    public ApiResponse createAdmin(@RequestBody AdminLoginRequest req) {
        try {
            return ApiResponse.ok(authService.createAdmin(req.getUsername(), req.getPassword()));
        } catch (IllegalArgumentException e) {
            // 你现在 adminLogin 里抛的是 IllegalArgumentException("BAD_CREDENTIALS" / ...)
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/leaderboard")
    public ApiResponse leaderboard(@RequestParam(required = false) Integer limit,
                                   @RequestParam(required = false) Integer offset) {
        return ApiResponse.ok(statisticsService.getLeaderboard(limit, offset));
    }

    @GetMapping("/get-student-detailed/{id}")
    public ApiResponse getDetailed(@PathVariable("id") Long studentId) {
        try {
            List<PracticeAttemptDTO> attempts = practiceService.getAttemptsByStudentId(studentId);
            return ApiResponse.ok(attempts);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-student-practiced-detailed/{attemptId}")
    public ApiResponse getStudentPracticedDetailed(@PathVariable("attemptId") Long attemptId) {
        try {
            List<PracticeAttemptDetailedDTO> out = practiceService.getDetailByAttemptId(attemptId);
            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }

    @GetMapping("/get-student-practice-statistics/{id}")
    public ApiResponse getStudentPracticedStatistics(@PathVariable("id") Long studentId) {
        try {
            StudentPracticeReportDTO out = adminPracticeService.getStudentPracticeReport(studentId);
            return ApiResponse.ok(out);
        } catch (Exception e) {
            return ApiResponse.error(e.getMessage());
        }
    }
}
