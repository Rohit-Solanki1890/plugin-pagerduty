package io.kestra.plugin.pagerduty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.TestRunner;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

@KestraTest
public class PagerDutyExecutionTest extends AbstractPagerDutyTest {

    @Inject
    protected TestRunner runner;

    @Inject
    protected LocalFlowRepositoryLoader repositoryLoader;

    @BeforeEach
    protected void init() throws IOException, URISyntaxException {
        repositoryLoader.load(Objects.requireNonNull(PagerDutyExecutionTest.class.getClassLoader().getResource("flows")));
        this.runner.run();
    }

    @Test
    void flowDefaultPayloadWhenNoCustomProperties() throws Exception {
        var failedExecution = runAndCaptureExecution(
            "main-flow-that-fails",
            "pagerduty"
        );

        var receivedData = waitForWebhookData(() -> FakeWebhookController.data, 5000);

        var objectMapper = new ObjectMapper();
        var receivedJson = objectMapper.readTree(receivedData);

        var expectedCustomDetails = objectMapper.readTree("""
            [
                {"title": "Flow ID", "value": "main-flow-that-fails"},
                {"title": "Execution Status", "value": "FAILED"},
                {"title": "Final task ID", "value": "failed"}
            ]
            """);

        assertThat(
            receivedJson.at("/payload/custom_details"),
            equalTo(expectedCustomDetails)
        );

        assertThat(receivedData, containsString(failedExecution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, containsString("Failed on task `failed`"));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"failed\"}"));
        assertThat(receivedData, containsString("Kestra PagerDuty alert"));
    }

    @Test
    void flowIncludeCustomMessageAndFields() throws Exception {
        var failedExecution = runAndCaptureExecution(
            "main-flow-that-fails-custom",
            "pagerduty-custom"
        );

        var receivedData = waitForWebhookData(() -> FakeWebhookController.data, 5000);

        assertThat(receivedData, containsString(failedExecution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, containsString("Failed on task `failed`"));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"failed\"}"));
        assertThat(receivedData, containsString("Kestra PagerDuty alert"));
        assertThat(receivedData, containsString("\"title\":\"Message\",\"value\":\"this is my custom message\""));
        assertThat(receivedData, containsString("\"title\":\"environment\",\"value\":\"test\""));
        assertThat(receivedData, containsString("\"title\":\"team\",\"value\":\"platform\""));
        assertThat(receivedData, containsString("\"title\":\"retries\",\"value\":4"));
        assertThat(receivedData, containsString("\"title\":\"urgent\",\"value\":false"));
    }

}
