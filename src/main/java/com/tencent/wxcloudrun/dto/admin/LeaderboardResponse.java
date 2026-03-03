package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class LeaderboardResponse {
    private long total;
    private List<StudentRankDTO> items;

    @Data
    public static class StudentRankDTO {
        private int rank;                 // 1,2,3...
        private Long studentId;
        // private String studentNoMasked;   // 学号
        private String name;              // 可选：如果你想保护隐私也可以脱敏
        private String college;
        private Long classId;
        private String className;
        private BigDecimal theta;
        private Integer practiceCount;    // 来自 student_ability_state
    }
}
