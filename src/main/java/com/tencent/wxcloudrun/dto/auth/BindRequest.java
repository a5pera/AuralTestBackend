package com.tencent.wxcloudrun.dto.auth;

import lombok.Data;

@Data
public class BindRequest {
    private String studentNo;
    private String name;
    private String college;
    private Long classId;
}