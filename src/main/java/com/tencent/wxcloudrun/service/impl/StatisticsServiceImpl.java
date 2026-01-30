package com.tencent.wxcloudrun.service.impl;

import com.tencent.wxcloudrun.dao.StudentAbilityStateMapper;
import com.tencent.wxcloudrun.dao.StudentMapper;
import com.tencent.wxcloudrun.dto.admin.LeaderboardResponse;
import com.tencent.wxcloudrun.model.auth.Student;
import com.tencent.wxcloudrun.model.user.StudentAbilityState;
import com.tencent.wxcloudrun.service.StatisticsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class StatisticsServiceImpl implements StatisticsService {
    private final StudentMapper studentMapper;
    private final StudentAbilityStateMapper studentAbilityStateMapper;

    public StatisticsServiceImpl(StudentMapper studentMapper,
                                 StudentAbilityStateMapper studentAbilityStateMapper) {
        this.studentMapper = studentMapper;
        this.studentAbilityStateMapper = studentAbilityStateMapper;
    }

    public LeaderboardResponse getLeaderboard(Integer limit, Integer offset) {
        int lim = (limit == null ? 50 : Math.max(1, Math.min(limit, 200)));
        int off = (offset == null ? 0 : Math.max(0, offset));

        long total = studentMapper.countLeaderboard();
        List<Student> rows = studentMapper.listLeaderboard(lim, off);

        List<LeaderboardResponse.StudentRankDTO> items = new ArrayList<>();
        int rank = off + 1;
        for (Student s : rows) {
            LeaderboardResponse.StudentRankDTO dto = new LeaderboardResponse.StudentRankDTO();
            dto.setRank(rank++);
            dto.setStudentId(s.getId());
            dto.setName(s.getName());
            dto.setCollege(s.getCollege());
            dto.setTheta(s.getTheta());
            StudentAbilityState sas = studentAbilityStateMapper.findByStudentId(s.getId());
            dto.setPracticeCount(sas.getPracticeCount());
            // dto.setPracticeCount(s.getPracticeCount() == null ? 0 : s.getPracticeCount());
            items.add(dto);
        }

        LeaderboardResponse resp = new LeaderboardResponse();
        resp.setTotal(total);
        resp.setItems(items);
        return resp;
    }
}
