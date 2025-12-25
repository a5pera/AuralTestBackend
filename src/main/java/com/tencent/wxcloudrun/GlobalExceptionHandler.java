package com.tencent.wxcloudrun;

import java.lang.Void;

import com.tencent.wxcloudrun.config.ApiResponse;
import com.tencent.wxcloudrun.config.BizException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)
    public ApiResponse handleBiz(BizException e) {
        return ApiResponse.error(e.getCode()); // 这里返回 {code:"DENY",...}
    }
}
