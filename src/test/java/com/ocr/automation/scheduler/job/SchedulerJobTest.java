package com.ocr.automation.scheduler.job;

import com.ocr.automation.scheduler.client.BackendClient;
import com.ocr.automation.scheduler.client.BackendUnavailableException;
import com.ocr.automation.scheduler.client.dto.ProcessingSummary;
import com.ocr.automation.scheduler.client.dto.RecoveryResult;
import com.ocr.automation.scheduler.config.SchedulerProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 잡의 핵심 계약: <b>어떤 경우에도 예외를 밖으로 내보내지 않는다.</b>
 *
 * <p>{@code @Scheduled} 메서드에서 예외가 새어나가면 해당 잡은 다시 스케줄되지 않는다.
 * backend 가 잠깐 죽었다고 스케줄러가 영영 멈추면 안 되므로 이 동작을 명시적으로 검증한다.
 */
class SchedulerJobTest {

    private BackendClient backendClient;
    private PendingDocumentDispatchJob dispatchJob;
    private StaleDocumentRecoveryJob recoveryJob;

    @BeforeEach
    void setUp() {
        backendClient = Mockito.mock(BackendClient.class);
        SchedulerProperties properties = new SchedulerProperties(
                new SchedulerProperties.Backend("http://localhost:8080", 2000, 60000),
                new SchedulerProperties.Jobs(
                        new SchedulerProperties.Jobs.PendingDispatch(true, 20),
                        new SchedulerProperties.Jobs.StaleRecovery(true)));
        dispatchJob = new PendingDocumentDispatchJob(backendClient, properties);
        recoveryJob = new StaleDocumentRecoveryJob(backendClient);
    }

    @Test
    void 설정된_배치_크기로_요청한다() {
        when(backendClient.processPending(20)).thenReturn(new ProcessingSummary(3, 3, 0, 0));

        dispatchJob.dispatch();

        verify(backendClient).processPending(20);
    }

    @Test
    void 처리할_문서가_없어도_조용히_끝난다() {
        when(backendClient.processPending(anyInt())).thenReturn(new ProcessingSummary(0, 0, 0, 0));

        assertThatCode(dispatchJob::dispatch).doesNotThrowAnyException();
    }

    @Test
    void backend_가_죽어도_잡은_예외를_던지지_않는다() {
        when(backendClient.processPending(anyInt()))
                .thenThrow(new BackendUnavailableException("연결 실패", new RuntimeException()));

        assertThatCode(dispatchJob::dispatch).doesNotThrowAnyException();
    }

    @Test
    void 예상치_못한_예외도_잡_밖으로_나가지_않는다() {
        when(backendClient.processPending(anyInt())).thenThrow(new IllegalStateException("알 수 없는 오류"));

        assertThatCode(dispatchJob::dispatch).doesNotThrowAnyException();
    }

    @Test
    void 응답이_null_이어도_터지지_않는다() {
        when(backendClient.processPending(anyInt())).thenReturn(null);

        assertThatCode(dispatchJob::dispatch).doesNotThrowAnyException();
    }

    @Test
    void 정체_문서를_회수한다() {
        when(backendClient.recoverStalled()).thenReturn(new RecoveryResult(2));

        recoveryJob.recover();

        verify(backendClient).recoverStalled();
    }

    @Test
    void 회수_잡도_backend_장애에_예외를_던지지_않는다() {
        doThrow(new BackendUnavailableException("연결 실패", new RuntimeException()))
                .when(backendClient).recoverStalled();

        assertThatCode(recoveryJob::recover).doesNotThrowAnyException();
    }

    @Test
    void 회수_잡도_null_응답에_터지지_않는다() {
        when(backendClient.recoverStalled()).thenReturn(null);

        assertThatCode(recoveryJob::recover).doesNotThrowAnyException();
    }
}
