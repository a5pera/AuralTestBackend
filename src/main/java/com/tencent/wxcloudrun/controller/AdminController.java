package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.dto.admin.LeaderboardResponse;
import com.tencent.wxcloudrun.dto.auth.AdminLoginRequest;
import com.tencent.wxcloudrun.service.AuthService;
import com.tencent.wxcloudrun.service.MaterialService;
import com.tencent.wxcloudrun.service.StatisticsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminController {
    private final AuthService authService;
    private final MaterialService materialService;
    private final StatisticsService statisticsService;

    public AdminController(AuthService authService, MaterialService materialService,
                           StatisticsService statisticsService) {
        this.authService = authService;
        this.materialService = materialService;
        this.statisticsService = statisticsService;
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


}
