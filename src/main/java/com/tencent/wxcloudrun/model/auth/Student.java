package com.tencent.wxcloudrun.model.auth;

import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class Student {
    public Long id;
    public Long rosterId;
    public String studentNo;
    public String name;
    public String college;
    public String wechatOpenid;
    public BigDecimal theta;
    public OffsetDateTime boundAt;
}
