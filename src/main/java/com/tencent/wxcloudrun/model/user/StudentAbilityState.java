package com.tencent.wxcloudrun.model.user;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class StudentAbilityState {
    private Long studentId;
    private Double thetaVar;
    private Integer practiceCount;
    private LocalDateTime updatedAt;
}