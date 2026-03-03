package com.tencent.wxcloudrun.model.user;

import lombok.Data;

import java.time.LocalDateTime;
@Data
public class Classes {
    private Long id;
    private String name;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
