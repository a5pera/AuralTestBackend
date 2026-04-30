package com.tencent.wxcloudrun.dto.rag;

import lombok.Data;

/**
 * 记录用户单道题的做题信息
 */
@Data
public class UserAnswerDTO {

    /**
     * 题号，对应前端的 qorder (如 1, 2, 3...)
     */
    private Integer qorder;

    /**
     * 用户所选的答案选项，对应前端的 optKey (如 "A", "B", "C", "D")
     */
    private String optKey;
}
