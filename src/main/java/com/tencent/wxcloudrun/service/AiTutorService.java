package com.tencent.wxcloudrun.service;

import com.tencent.wxcloudrun.dto.rag.AiAnalysisRequestDTO;
import com.tencent.wxcloudrun.dto.rag.AiAnalysisResponseDTO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiTutorService {

    // 从 application.yml 读取 Python 服务的地址，如果没有配置则默认 localhost:8000
    @Value("${ai.rag-server.url:http://localhost:8000/api/analysis/analyze}")
    private String ragServerUrl;

    private final RestTemplate restTemplate;

    public AiTutorService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * 调用 Python 大模型服务获取分析报告
     */
    public String getAiAnalysis(AiAnalysisRequestDTO pythonRequest) {
        try {
            // 向 Python 服务发起 POST 请求
            ResponseEntity<AiAnalysisResponseDTO> response = restTemplate.postForEntity(
                    ragServerUrl,
                    pythonRequest,
                    AiAnalysisResponseDTO.class
            );

            AiAnalysisResponseDTO body = response.getBody();
            if (body != null && body.isSuccess()) {
                return body.getAnalysis(); // 返回大模型生成的分析文本
            } else {
                return "AI老师暂时无法解析该题目，可能题库中未找到相关内容。";
            }
        } catch (Exception e) {
            // 降级兜底：不管 Python 挂了、超时了，都不能让 Java 报错崩溃
            System.err.println("调用 Python RAG 服务失败: " + e.getMessage());
            return "AI 服务连接超时或正在维护中，请稍后再试。";
        }
    }
}
