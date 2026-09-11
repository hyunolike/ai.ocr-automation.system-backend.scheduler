package com.ocr.automation.scheduler.client.dto;

/**
 * backend 의 배치 처리 결과.
 *
 * @param picked    처리 대상으로 집어든 문서 수
 * @param completed OCR 성공 수
 * @param failed    OCR 실패 수 (재시도 예정 포함)
 * @param skipped   다른 인스턴스가 먼저 선점해 건너뛴 수
 */
public record ProcessingSummary(int picked, int completed, int failed, int skipped) {

    public boolean didNothing() {
        return picked == 0;
    }
}
