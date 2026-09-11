package com.ocr.automation.scheduler.client.dto;

/**
 * 정체 문서 회수 결과.
 *
 * @param recovered 회수한 문서 수
 */
public record RecoveryResult(int recovered) {
}
