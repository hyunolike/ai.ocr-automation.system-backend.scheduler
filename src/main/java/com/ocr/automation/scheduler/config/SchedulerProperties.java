package com.ocr.automation.scheduler.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;

/**
 * 스케줄러 설정 (설정 서버의 ocr-scheduler.yml 에서 내려온다).
 *
 * <p>잡 주기를 코드가 아니라 설정으로 두는 이유: 운영 중에 주기를 조정하려면
 * 재배포 없이 설정만 바꿔야 하기 때문이다.
 */
@ConfigurationProperties(prefix = "ocr.scheduler")
public record SchedulerProperties(
        @DefaultValue Backend backend,
        @DefaultValue Jobs jobs) {

    /**
     * @param connectTimeoutMillis 연결 타임아웃
     * @param readTimeoutMillis    응답 타임아웃. OCR 배치가 오래 걸리므로 넉넉히 잡는다
     */
    public record Backend(
            @DefaultValue("http://localhost:8080") String baseUrl,
            @DefaultValue("2000") long connectTimeoutMillis,
            @DefaultValue("60000") long readTimeoutMillis) {

        public Duration connectTimeout() {
            return Duration.ofMillis(connectTimeoutMillis);
        }

        public Duration readTimeout() {
            return Duration.ofMillis(readTimeoutMillis);
        }
    }

    public record Jobs(
            @DefaultValue PendingDispatch pendingDispatch,
            @DefaultValue StaleRecovery staleRecovery) {

        /**
         * 대기 문서를 backend 의 OCR 처리로 넘기는 잡.
         *
         * @param batchSize 한 번에 넘길 문서 수
         */
        public record PendingDispatch(
                @DefaultValue("true") boolean enabled,
                @DefaultValue("20") int batchSize) {
        }

        /**
         * 처리 도중 중단되어 PROCESSING 에 멈춘 문서를 회수하는 잡.
         */
        public record StaleRecovery(
                @DefaultValue("true") boolean enabled) {
        }
    }
}
