import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.graalvm.collections.Pair;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildCounterTest {

    @Test
    public void testBuildCounterWithoutUnit() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String run = "basicsimulation_20250616094945181";
        String snakeCaseMetricName = "all_requests_number_of_requests";
        long counterValue = 1L;

        // When
        Counter counter = gatlingReporter.buildCounter(prometheusRegistry,  snakeCaseMetricName,"run","status").getLeft();
        counter.labelValues(run,"ok").inc(counterValue);
        // Then
        assertThat(counter.getPrometheusName()).isEqualTo(snakeCaseMetricName);
        ;
        assertThat(counter.labelValues(run,"ok").getLongValue()).isEqualTo(counterValue);
    }

    @Test
    public void testBuildCounterWithUnit() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String run = "basicsimulation_20250616094945181";
        String snakeCaseMetricName = "all_requests_mean_response_time_ok";
        long counterValue = 4235L;

        // When
        Pair<Counter, Boolean> pair = gatlingReporter.buildCounter(prometheusRegistry, snakeCaseMetricName, "run", "status");
        Counter counter = pair.getLeft();
        counter.labelValues(run,"ok").inc(pair.getRight()?counterValue/1000:counterValue);
        // Then
        assertThat(counter.getPrometheusName()).isEqualTo(snakeCaseMetricName+"_seconds");
        ;
        assertThat(counter.labelValues(run,"ok").getLongValue()).isEqualTo(counterValue/1000);
    }
}
