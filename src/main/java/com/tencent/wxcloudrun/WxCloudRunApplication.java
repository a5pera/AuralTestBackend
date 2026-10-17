package com.tencent.wxcloudrun;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = {"com.tencent.wxcloudrun.dao"})
public class WxCloudRunApplication {  

  public static void main(String[] args) {
    // 启动 Spring Boot 应用并加载后端服务配置。
    // 数据访问接口由 MapperScan 统一扫描并注册。
    SpringApplication.run(WxCloudRunApplication.class, args);
  }
}
