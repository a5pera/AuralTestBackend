package com.tencent.wxcloudrun.config;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    public final String code;
    public final HttpStatus status;

    private ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static ApiException badRequest(String code, String msg) { return new ApiException(HttpStatus.BAD_REQUEST, code, msg); }
    public static ApiException unauthorized(String code, String msg) { return new ApiException(HttpStatus.UNAUTHORIZED, code, msg); }
    public static ApiException notFound(String code, String msg) { return new ApiException(HttpStatus.NOT_FOUND, code, msg); }
    public static ApiException conflict(String code, String msg) { return new ApiException(HttpStatus.CONFLICT, code, msg); }
}
