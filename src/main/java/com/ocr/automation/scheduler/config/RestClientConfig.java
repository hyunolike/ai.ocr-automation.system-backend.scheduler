package com.ocr.automation.scheduler.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * backend 호출용 RestClient.
 *
 * <p>타임아웃을 반드시 건다. 기본값(무제한)으로 두면 backend 가 응답하지 않을 때
 * 스케줄러 스레드가 영원히 묶여 다음 주기가 아예 돌지 않는다.
 * 특히 읽기 타임아웃은 OCR 배치가 오래 걸리는 것을 감안해 넉넉히 잡는다.
 */
@Configuration
public class RestClientConfig {

    @Bean
    public RestClient backendRestClient(SchedulerProperties properties, RestClient.Builder builder) {
        SchedulerProperties.Backend backend = properties.backend();
        return builder
                .baseUrl(backend.baseUrl())
                .requestFactory(requestFactory(backend))
                .build();
    }

    private ClientHttpRequestFactory requestFactory(SchedulerProperties.Backend backend) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(backend.connectTimeout());
        factory.setReadTimeout(backend.readTimeout());
        return factory;
    }
}
