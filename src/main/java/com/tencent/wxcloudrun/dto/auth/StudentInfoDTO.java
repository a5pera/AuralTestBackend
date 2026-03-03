package com.tencent.wxcloudrun.dto.auth;

import lombok.Data;

@Data
public class StudentInfoDTO {
    private Long Id;
    private String name;
    private String college;
    private Long classId;
    private String className;
}
