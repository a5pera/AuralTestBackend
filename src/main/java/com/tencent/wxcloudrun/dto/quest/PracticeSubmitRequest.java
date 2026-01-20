package com.tencent.wxcloudrun.dto.quest;

import lombok.Data;

import java.util.List;

@Data
public class PracticeSubmitRequest {
    private Long materialId;
    private List<AnswerDTO> answers;

    @Data
    public static class AnswerDTO {
        private Long questionId;
        private String selectedKey; //  "A"/"B"/"C"/"D"
    }
}
