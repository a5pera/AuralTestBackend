package com.tencent.wxcloudrun.dto.rag;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 传给 Python 端的具体错题信息
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WrongAnswerDTO {

    /**
     * 错题的题号 (对应 Python 里的 questionNo)
     */
    private Integer questionNo;

    /**
     * 学生的错误答案 (对应 Python 里的 studentAnswer)
     */
    private String studentAnswer;
}
