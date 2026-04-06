package com.tencent.wxcloudrun.controller;

import com.tencent.wxcloudrun.config.ApiResponse; // 导入微信云托管默认的返回类
import com.tencent.wxcloudrun.dto.rag.AiAnalysisRequestDTO;
import com.tencent.wxcloudrun.dto.rag.AiAnalysisSubmitDTO;
import com.tencent.wxcloudrun.dto.rag.UserAnswerDTO;
import com.tencent.wxcloudrun.dto.rag.WrongAnswerDTO;
import com.tencent.wxcloudrun.service.AiTutorService;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/api/ai")
public class AiAnalysisController {

    // 【修复警告】去除 @Autowired，使用 final 关键字
    private final AiTutorService aiTutorService;

    // 【修复警告】使用官方推荐的构造器注入方式
    public AiAnalysisController(AiTutorService aiTutorService) {
        this.aiTutorService = aiTutorService;
    }

    /**
     * 前端提交试卷信息，获取 AI 错题解析
     */
    @PostMapping("/analyze-wrong-answers")
    // 【修复报错】将 Result 替换为云托管模板自带的 ApiResponse
    public ApiResponse analyzeWrongAnswers(@RequestBody AiAnalysisSubmitDTO request) {

        System.out.println("====== 成功接收到前端请求！题目：" + request.getMaterialTitle() + " ======");

        // 1. 获取错题列表
        List<WrongAnswerDTO> wrongAnswers = findWrongAnswers(request);

        if (wrongAnswers.isEmpty()) {
            // 【修复报错】使用 ApiResponse.ok() 返回成功数据
            return ApiResponse.ok("太棒了！您全答对了，无需错题分析。");
        }

        // 2. 组装发给 Python 端的请求对象
        AiAnalysisRequestDTO pythonRequest = new AiAnalysisRequestDTO();
        pythonRequest.setTitle(request.getMaterialTitle());
        pythonRequest.setWrongAnswers(wrongAnswers);

        // 3. 调用 AI Service 获取分析结果
        String aiAnalysisResult = aiTutorService.getAiAnalysis(pythonRequest);

        // 4. 返回给前端
        return ApiResponse.ok(aiAnalysisResult);
    }

    /**
     * 注意：因为前端现在只把错题发过来了，所以后端无需再查数据库。
     * 直接将前端的数据格式转换为 Python 需要的格式即可！
     */
    private List<WrongAnswerDTO> findWrongAnswers(AiAnalysisSubmitDTO request) {
        List<WrongAnswerDTO> wrongAnswers = new ArrayList<>();

        if (request.getUserAnswers() == null) {
            return wrongAnswers;
        }

        // 直接转换
        for (UserAnswerDTO ans : request.getUserAnswers()) {
            WrongAnswerDTO wrongAnswer = new WrongAnswerDTO();
            wrongAnswer.setQuestionNo(ans.getQorder());
            wrongAnswer.setStudentAnswer(ans.getOptKey());
            wrongAnswers.add(wrongAnswer);
        }

        return wrongAnswers;
    }
}