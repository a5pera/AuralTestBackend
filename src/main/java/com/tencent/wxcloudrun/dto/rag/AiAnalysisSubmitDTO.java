package com.tencent.wxcloudrun.dto.rag;
import lombok.Data;
import java.util.List;
/**
 * 接收前端传来的交卷信息（用于 AI 错题分析）
 */
@Data
public class AiAnalysisSubmitDTO {

    /**
     * 题目/材料名称，例如："2023年12月大学英语四级考试真题(二)第8、9、10、11题"
     * 对应前端传来的 materialTitle
     */
    private String materialTitle;

    /**
     * 用户的做题记录列表
     */
    private List<UserAnswerDTO> userAnswers;
}