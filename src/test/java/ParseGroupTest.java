import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import org.assertj.core.api.Assertions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertThrows;

public class ParseGroupTest {

    @Test
    public void testParseGroupNominalCase() {
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
        //We check that the counter is registered in the Prometheus registry
        assertThat(prometheusRegistry.scrape().stream()
                .anyMatch(snapshot->
                        snapshot.getMetadata().getPrometheusName()
                                .equals("all_requests_t_lower_than_800_ms_count"))).isTrue();
    }

    @Test
    public void testParseGroupWithNullValue() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        PrometheusRegistry prometheusRegistry = new PrometheusRegistry();
        String parentName=null;
        String memberAsString=null;
        long counterValue=0L;

        // When
        assertThrows(NullPointerException.class,()->gatlingReporter.parseGroup(prometheusRegistry,parentName,memberAsString,counterValue));

    }
}
