package com.ocr.automation.scheduler.job;

import com.ocr.automation.scheduler.client.BackendClient;
import com.ocr.automation.scheduler.client.BackendUnavailableException;
import com.ocr.automation.scheduler.client.dto.DispatchResult;
import com.ocr.automation.scheduler.config.SchedulerProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 대기 중인 문서를 주기적으로 OCR 처리에 넘긴다.
 *
 * <p>backend 는 접수만 하고 즉시 응답하므로 이 호출은 짧다. 처리 결과는
 * 이 잡이 기다리지 않으며, 문서 상태로 따로 확인한다.
 *
 * <p>주기는 {@code fixedDelayString} 이다. fixedRate 가 아닌 이유:
 * 처리가 주기보다 오래 걸리면 fixedRate 는 호출을 밀어 넣어 backend 를 더 밀어붙인다.
 * fixedDelay 는 <b>이전 실행이 끝난 뒤부터</b> 주기를 세므로 밀리지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "ocr.scheduler.jobs.pending-dispatch.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class PendingDocumentDispatchJob {

    private final BackendClient backendClient;
    private final SchedulerProperties properties;

    @Scheduled(
            fixedDelayString = "${ocr.scheduler.jobs.pending-dispatch.fixed-delay:30s}",
            initialDelayString = "${ocr.scheduler.jobs.pending-dispatch.initial-delay:10s}")
    public void dispatch() {
        int batchSize = properties.jobs().pendingDispatch().batchSize();
        try {
            DispatchResult result = backendClient.processPending(batchSize);
            if (result == null || result.didNothing()) {
                log.debug("처리할 대기 문서가 없습니다");
                return;
            }
            if (result.isBackendSaturated()) {
                // 처리가 유입을 못 따라가고 있다. 주기를 늘려도 해결되지 않는다 —
                // backend 의 동시성을 올리거나 인스턴스를 늘려야 한다.
                log.warn("backend 워커 큐가 포화 상태입니다: queued={}, rejected={}, skipped={}",
                        result.queued(), result.rejected(), result.skipped());
                return;
            }
            log.info("대기 문서 접수 완료: queued={}, skipped={}", result.queued(), result.skipped());
        } catch (BackendUnavailableException e) {
            // 다음 주기에 다시 시도하면 되므로 잡을 죽이지 않는다.
            log.warn("backend 호출 실패. 다음 주기에 재시도합니다: {}", e.getMessage());
        } catch (Exception e) {
            // 여기서 예외가 새어나가면 이 잡은 다시 스케줄되지 않는다.
            log.error("대기 문서 처리 중 예상치 못한 오류", e);
        }
    }
}
