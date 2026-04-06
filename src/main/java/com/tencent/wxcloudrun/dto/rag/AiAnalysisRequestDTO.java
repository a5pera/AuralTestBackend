package com.tencent.wxcloudrun.dto.rag;

import lombok.Data;
import java.util.List;

/**
 * 发送给 Python RAG 服务的请求体
 */
@Data
public class AiAnalysisRequestDTO {

    /**
     * 试卷名称，直接把前端的 materialTitle 传过来
     */
    private String title;

    /**
     * 筛选出来的错题列表
     */
    private List<WrongAnswerDTO> wrongAnswers;
}