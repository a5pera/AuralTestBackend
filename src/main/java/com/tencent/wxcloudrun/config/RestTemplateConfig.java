package com.tencent.wxcloudrun.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        // 连接超时时间：10秒
        factory.setConnectTimeout(10000);
        // 读取超时时间：60秒（由于调用Qwen14B生成文本较慢，这里必须设置长一点，建议60-120秒）
        factory.setReadTimeout(60000);
        return new RestTemplate(factory);
    }
}