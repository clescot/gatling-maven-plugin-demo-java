import org.junit.Test;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class GetLastGatlingDirectoryTest {
    @Test
    public void testGetLastGatlingDirectory() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String expectedDirectory = "target"+File.separator+"gatling"+File.separator+"basicsimulation-20250616094945181";
        Path absolutePath = Paths.get("").toAbsolutePath();
        // When
        String lastGatlingDirectory = gatlingReporter.getLastGatlingDirectory().getPath();

        // Then
        assertThat(lastGatlingDirectory).isEqualTo(absolutePath+ File.separator+expectedDirectory);
    }
}
