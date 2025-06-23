import org.graalvm.polyglot.Value;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

public class GetStatsFromRootValueTest {

    @Test
    public void testGetStatsFromRootValue() throws IOException {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        File gatlingDirectory = gatlingReporter.getLastGatlingDirectory();
        // When
        Value stats = gatlingReporter.getStatsFromRootValue(gatlingDirectory);

        // Then
        assertThat(stats).isNotNull();
    }

}
