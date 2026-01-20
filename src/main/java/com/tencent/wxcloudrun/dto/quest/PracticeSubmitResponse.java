package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

@Data
public class PracticeSubmitResponse {
    private Long materialId;
    private int total;
    private int correct;
    private double thetaOld;
    private double thetaNew;
    private double varOld;
    private double varNew;
    private int practiceCountNew;
}
