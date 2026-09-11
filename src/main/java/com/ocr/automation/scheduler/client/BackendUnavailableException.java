package com.ocr.automation.scheduler.client;

/**
 * backend 호출 실패.
 *
 * <p>스케줄러 입장에서는 복구 가능한 상황이다. 다음 주기에 다시 시도하면 되므로
 * 잡을 중단시키지 않는다.
 */
public class BackendUnavailableException extends RuntimeException {

    public BackendUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
