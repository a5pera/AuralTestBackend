package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.admin.LeaderboardResponse;

public interface StatisticsService {
    LeaderboardResponse getLeaderboard(Integer limit, Integer offset);
}
