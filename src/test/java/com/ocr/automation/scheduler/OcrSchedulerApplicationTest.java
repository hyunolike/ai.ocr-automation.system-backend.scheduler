package com.ocr.automation.scheduler;

import com.ocr.automation.scheduler.config.SchedulerProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 컨텍스트가 뜨는지, 설정이 제대로 바인딩되는지 확인한다.
 * 잡은 테스트 설정에서 꺼둔 상태다.
 */
@SpringBootTest
class OcrSchedulerApplicationTest {

    @Autowired
    private SchedulerProperties properties;

    @Autowired
    private RestClient backendRestClient;

    @Test
    void 설정이_바인딩된다() {
        assertThat(properties.backend().baseUrl()).isEqualTo("http://localhost:18080");
        assertThat(properties.backend().connectTimeout().toMillis()).isEqualTo(500);
        assertThat(properties.jobs().pendingDispatch().batchSize()).isEqualTo(20);
    }

    @Test
    void backend_restClient_빈이_등록된다() {
        assertThat(backendRestClient).isNotNull();
    }
}
