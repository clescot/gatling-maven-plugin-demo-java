import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ParseGroupTest {

    @Test
    public void testParseGroup() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String parentName="all_requests";
        String memberAsString="t < 800 ms";
        long counterValue=1L;
//        // When
        Counter counter = gatlingReporter.parseGroup(prometheusRegistry,parentName,memberAsString,counterValue);
//
//        // Then
        assertThat(counter.getPrometheusName()).isEqualTo("all_requests_t_lower_than_800_ms_count");
        assertThat(counter.getLongValue()).isEqualTo(1L);
    }
}
