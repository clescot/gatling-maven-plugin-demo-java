import io.prometheus.metrics.core.metrics.Counter;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class GetGatlingMetricsFromLastDirectoryTest {

    @Test
    public void testExtractGatlingMetricsFromLastDirectory() throws IOException {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();

        // When
        List<Counter> counters = gatlingReporter.getGatlingMetricsFromLastDirectory();

        // Then
       assertThat(counters).isNotNull();
       assertThat(counters).isNotEmpty();
    }
}
