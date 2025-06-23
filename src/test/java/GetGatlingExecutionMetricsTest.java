import io.prometheus.metrics.core.metrics.Counter;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GetGatlingExecutionMetricsTest {

    @Test
    public void TestGetGatlingExecutionMetrics() throws URISyntaxException, IOException {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        URL flatUrl = Thread.currentThread().getContextClassLoader().getResource("stats_js/flat_stats_js");
        File statsJsFile = new File(flatUrl.toURI());
        if(!statsJsFile.exists()){
            throw new IllegalStateException("stats_js/flat_stats.js file not found in resources");
        }
        // When
        List<Counter> metrics = gatlingReporter.getGatlingExecutionMetrics(statsJsFile);

        // Then
        assertThat(metrics).isNotNull();
        assertThat(metrics).isNotEmpty();
        metrics.stream().map(counter -> counter.getPrometheusName()).
                forEach(name -> System.out.println(name));
    }
}
