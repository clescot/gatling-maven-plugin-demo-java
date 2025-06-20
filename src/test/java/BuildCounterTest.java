import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
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
        Counter counter = gatlingReporter.buildCounter(prometheusRegistry, run, snakeCaseMetricName, counterValue+"");

        // Then
        assertThat(counter.getPrometheusName()).isEqualTo(snakeCaseMetricName);
        ;
        assertThat(counter.labelValues(run).getLongValue()).isEqualTo(counterValue);
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
        Counter counter = gatlingReporter.buildCounter(prometheusRegistry, run, snakeCaseMetricName, counterValue+"");

        // Then
        assertThat(counter.getPrometheusName()).isEqualTo(snakeCaseMetricName+"_seconds");
        ;
        assertThat(counter.labelValues(run).getLongValue()).isEqualTo(counterValue/1000);
    }
}
