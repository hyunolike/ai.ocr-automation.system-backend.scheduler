package com.ocr.automation.scheduler.client;

import com.ocr.automation.scheduler.client.dto.DispatchResult;
import com.ocr.automation.scheduler.client.dto.RecoveryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * backend 의 내부 OCR API 를 호출하는 클라이언트.
 *
 * <p>스케줄러는 <b>"언제 돌릴지"만</b> 안다. OCR 엔진도 스토리지도 DB 도 모른다.
 * 그 덕분에 엔진을 바꾸거나 스토리지를 옮겨도 이 서비스는 손댈 일이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BackendClient {

    private final RestClient backendRestClient;

    /**
     * 대기 중인 문서를 워커 풀에 접수하도록 요청한다.
     *
     * <p>backend 는 처리를 기다리지 않고 202 로 즉시 응답한다. 그래서 이 호출은 짧다.
     */
    public DispatchResult processPending(int batchSize) {
        try {
            return backendRestClient.post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/internal/v1/ocr/process-pending")
                            .queryParam("batchSize", batchSize)
                            .build())
                    .retrieve()
                    .body(DispatchResult.class);
        } catch (RestClientException e) {
            throw new BackendUnavailableException(
                    "대기 문서 처리 요청에 실패했습니다: batchSize=" + batchSize, e);
        }
    }

    /** 중단된 채 PROCESSING 에 멈춘 문서를 회수하도록 요청한다. */
    public RecoveryResult recoverStalled() {
        try {
            return backendRestClient.post()
                    .uri("/internal/v1/ocr/recover-stalled")
                    .retrieve()
                    .body(RecoveryResult.class);
        } catch (RestClientException e) {
            throw new BackendUnavailableException("정체 문서 회수 요청에 실패했습니다", e);
        }
    }
}
