import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class BuildCounterTest {

    @Test
    public void testBuildCounter() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String run = "basicsimulation_20250616094945181";
        String snakeCaseMetricName = "all_requests_number_of_requests";
        long counterValue = 1L;

        // When
        Counter counter = gatlingReporter.buildCounter(prometheusRegistry, run, snakeCaseMetricName, counterValue+"");

        // Then
        assertThat(counter.getPrometheusName()).isEqualTo("all_requests_number_of_requests");
        ;
        assertThat(counter.labelValues(run).getLongValue()).isEqualTo(counterValue);
    }
}
