package io.kestra.plugin.pagerduty;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Objects;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.kestra.core.junit.annotations.KestraTest;
import io.kestra.core.repositories.LocalFlowRepositoryLoader;
import io.kestra.core.runners.TestRunner;

import jakarta.inject.Inject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

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

        String receivedData = waitForWebhookData(() -> FakeWebhookController.data, 5000);

        assertThat(receivedData, containsString(failedExecution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, containsString("Failed on task `failed`"));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"failed\"}"));
        assertThat(receivedData, containsString("Kestra PagerDuty alert"));
        assertThat(receivedData, not(containsString("\"title\":\"Message\",\"value\":\"this is my custom message\"")));
        assertThat(receivedData, not(containsString("\"title\":\"environment\",\"value\":\"test\"")));
        assertThat(receivedData, not(containsString("\"title\":\"team\",\"value\":\"platform\"")));
        assertThat(receivedData, not(containsString("\"title\":\"retries\",\"value\":\"4\"")));
        assertThat(receivedData, not(containsString("\"title\":\"urgent\",\"value\":\"false\"")));
    }

    @Test
    void flowIncludeCustomMessageAndFields() throws Exception {
        FakeWebhookController.data = null;
        var failedExecution = runAndCaptureExecution(
            "main-flow-that-fails-custom",
            "pagerduty-custom"
        );

        String receivedData = waitForWebhookData(() -> FakeWebhookController.data, 5000);

        assertThat(receivedData, containsString(failedExecution.getId()));
        assertThat(receivedData, containsString("https://mysuperhost.com/kestra/ui"));
        assertThat(receivedData, containsString("Failed on task `failed`"));
        assertThat(receivedData, containsString("{\"title\":\"Final task ID\",\"value\":\"failed\"}"));
        assertThat(receivedData, containsString("Kestra PagerDuty alert"));
        assertThat(receivedData, containsString("\"title\":\"Message\",\"value\":\"this is my custom message\""));
        assertThat(receivedData, containsString("\"title\":\"environment\",\"value\":\"test\""));
        assertThat(receivedData, containsString("\"title\":\"team\",\"value\":\"platform\""));
        assertThat(receivedData, not(containsString("\\\\n")));
        assertThat(receivedData, containsString("\"title\":\"retries\",\"value\":\"4\""));
        assertThat(receivedData, containsString("\"title\":\"urgent\",\"value\":\"false\""));
    }

}
