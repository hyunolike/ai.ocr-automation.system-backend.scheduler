package com.ocr.automation.scheduler.config;

import lombok.extern.slf4j.Slf4j;
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
 *
 * <p>내부 토큰도 여기서 한 번만 건다. 호출 지점마다 붙이게 두면 언젠가 하나를 빠뜨린다.
 */
@Slf4j
@Configuration
public class RestClientConfig {

    /** backend 의 InternalTokenAuthenticationFilter 가 읽는 헤더 이름. */
    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Bean
    public RestClient backendRestClient(SchedulerProperties properties, RestClient.Builder builder) {
        SchedulerProperties.Backend backend = properties.backend();
        if (backend.usesDevToken()) {
            log.warn("내부 토큰이 개발용 기본값입니다. 운영에서는 ocr.scheduler.backend.internal-token 을 반드시 바꾸세요.");
        }
        return builder
                .baseUrl(backend.baseUrl())
                // 모든 호출에 내부 토큰을 붙인다. 호출 지점마다 잊지 않도록 여기서 한 번만 건다.
                .defaultHeader(INTERNAL_TOKEN_HEADER, backend.internalToken())
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
