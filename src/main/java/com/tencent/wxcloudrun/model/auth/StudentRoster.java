package com.tencent.wxcloudrun.model.auth;

import lombok.Data;

@Data
public class StudentRoster {
    public Long id;
    public String studentNo;
    public String name;
    public String college;
    public boolean isActive;
    public Long classId;
}
