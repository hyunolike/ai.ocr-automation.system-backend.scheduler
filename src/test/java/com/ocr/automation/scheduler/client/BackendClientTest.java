package com.ocr.automation.scheduler.client;

import com.ocr.automation.scheduler.client.dto.DispatchResult;
import com.ocr.automation.scheduler.client.dto.RecoveryResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class BackendClientTest {

    private MockRestServiceServer server;
    private BackendClient backendClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://backend:8080");
        server = MockRestServiceServer.bindTo(builder).build();
        backendClient = new BackendClient(builder.build());
    }

    @Test
    void 대기_문서_접수를_요청하고_결과를_읽는다() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/process-pending?batchSize=20"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"queued":5,"rejected":0,"skipped":1}
                              """));

        DispatchResult result = backendClient.processPending(20);

        assertThat(result.queued()).isEqualTo(5);
        assertThat(result.rejected()).isZero();
        assertThat(result.skipped()).isEqualTo(1);
        assertThat(result.didNothing()).isFalse();
        assertThat(result.isBackendSaturated()).isFalse();
        server.verify();
    }

    @Test
    void 접수할_문서가_없으면_didNothing() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/process-pending?batchSize=10"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"queued":0,"rejected":0,"skipped":0}
                              """));

        assertThat(backendClient.processPending(10).didNothing()).isTrue();
    }

    @Test
    void 거부된_문서가_있으면_포화로_판정한다() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/process-pending?batchSize=20"))
                .andRespond(withStatus(HttpStatus.ACCEPTED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("""
                              {"queued":2,"rejected":18,"skipped":0}
                              """));

        DispatchResult result = backendClient.processPending(20);

        assertThat(result.isBackendSaturated()).isTrue();
        assertThat(result.didNothing()).isFalse();
    }

    @Test
    void 정체_문서_회수를_요청한다() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/recover-stalled"))
                .andExpect(method(org.springframework.http.HttpMethod.POST))
                .andRespond(withSuccess("""
                        {"recovered":3}
                        """, MediaType.APPLICATION_JSON));

        RecoveryResult result = backendClient.recoverStalled();

        assertThat(result.recovered()).isEqualTo(3);
        server.verify();
    }

    @Test
    void backend_가_5xx_면_복구_가능한_예외로_바꾼다() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/process-pending?batchSize=20"))
                .andRespond(withServerError());

        assertThatThrownBy(() -> backendClient.processPending(20))
                .isInstanceOf(BackendUnavailableException.class)
                .hasMessageContaining("대기 문서 처리 요청에 실패했습니다");
    }

    @Test
    void backend_가_4xx_면_복구_가능한_예외로_바꾼다() {
        server.expect(requestTo("http://backend:8080/internal/v1/ocr/recover-stalled"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> backendClient.recoverStalled())
                .isInstanceOf(BackendUnavailableException.class);
    }
}
