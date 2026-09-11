package com.ocr.automation.scheduler;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * OCR 자동화 시스템 스케줄러.
 *
 * <p>"언제 처리할지"만 결정한다. OCR 엔진도 스토리지도 DB 도 모르고,
 * backend 의 내부 API 를 호출하기만 한다.
 */
@SpringBootApplication
public class OcrSchedulerApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcrSchedulerApplication.class, args);
    }
}
