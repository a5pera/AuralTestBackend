package com.tencent.wxcloudrun.dto.rag;

import lombok.Data;

/**
 * 接收 Python RAG 服务返回的结果
 */
@Data
public class AiAnalysisResponseDTO {

    /**
     * RAG 服务是否成功处理并匹配到题目
     */
    private boolean success;

    /**
     * 大模型生成的详细错题分析文本
     */
    private String analysis;

    /**
     * 成功匹配到知识库的题目数量
     */
    private Integer matchedQuestions;
}