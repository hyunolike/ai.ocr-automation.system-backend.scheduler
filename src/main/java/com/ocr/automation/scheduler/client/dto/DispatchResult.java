package com.ocr.automation.scheduler.client.dto;

/**
 * backend 의 대기 문서 <b>접수</b> 결과.
 *
 * <p>처리 결과가 아니다. backend 는 작업을 워커 풀에 넣고 즉시 응답하므로,
 * 실제 성공·실패는 문서 상태로 확인해야 한다.
 *
 * @param queued   워커 큐에 들어간 문서 수
 * @param rejected 큐가 가득 차 넣지 못한 수. 0 보다 크면 처리가 유입을 못 따라가는 것이다
 * @param skipped  다른 인스턴스가 먼저 선점해 건너뛴 수
 */
public record DispatchResult(int queued, int rejected, int skipped) {

    public boolean didNothing() {
        return queued == 0 && rejected == 0;
    }

    /** 큐가 포화 상태인가. 스케줄러가 경고를 남기는 기준이다. */
    public boolean isBackendSaturated() {
        return rejected > 0;
    }
}
