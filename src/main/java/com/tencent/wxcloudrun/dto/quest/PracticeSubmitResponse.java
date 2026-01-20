package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
    private List<CorrectAnswerDTO> answers;

    public PracticeSubmitResponse() {
    }

    @Data
    public static class CorrectAnswerDTO {
        private Long questionId;
        private String correctAnswer;
        private String selectedAnswer;
        private Boolean isCorrect;
    }
}
