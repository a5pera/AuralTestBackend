package com.tencent.wxcloudrun.config;

public class BizException extends RuntimeException {
    private final String code;

    public BizException(String code) {
        super(code);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    public static BizException notBound() {
        return new BizException("NOT_BOUND");
    }

    public static BizException deny() {
        return new BizException("DENY");
    }

    public static BizException missingParams() {
        return new BizException("MISSING_PARAMS");
    }

    public static BizException alreadyBound() {
        return new BizException("ALREADY_BOUND");
    }

    public static BizException notAllowed() {
        return new BizException("NOT_ALLOWED");
    }
}
