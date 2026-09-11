package com.ocr.automation.scheduler.job;

import com.ocr.automation.scheduler.client.BackendClient;
import com.ocr.automation.scheduler.client.BackendUnavailableException;
import com.ocr.automation.scheduler.client.dto.RecoveryResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 처리 도중 인스턴스가 죽어 PROCESSING 에 멈춘 문서를 주기적으로 회수한다.
 *
 * <p>이 잡이 없으면 그런 문서는 영영 처리되지 않는다. 대기 문서 처리 잡보다
 * 눈에 덜 띄지만 파이프라인이 막히지 않게 하는 것은 이쪽이다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        name = "ocr.scheduler.jobs.stale-recovery.enabled",
        havingValue = "true",
        matchIfMissing = true)
public class StaleDocumentRecoveryJob {

    private final BackendClient backendClient;

    @Scheduled(cron = "${ocr.scheduler.jobs.stale-recovery.cron:0 0 * * * *}")
    public void recover() {
        try {
            RecoveryResult result = backendClient.recoverStalled();
            if (result == null || result.recovered() == 0) {
                log.debug("회수할 정체 문서가 없습니다");
                return;
            }
            log.warn("정체 문서를 회수했습니다: recovered={}", result.recovered());
        } catch (BackendUnavailableException e) {
            log.warn("backend 호출 실패. 다음 주기에 재시도합니다: {}", e.getMessage());
        } catch (Exception e) {
            log.error("정체 문서 회수 중 예상치 못한 오류", e);
        }
    }
}
