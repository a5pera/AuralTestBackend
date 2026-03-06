package com.tencent.wxcloudrun.dto.admin;

import lombok.Data;

@Data
public class ClassStudentCountDTO {
    private Long classId;
    private Long total;      // roster 总人数
    private Long bound;      // 已绑定人数
}
