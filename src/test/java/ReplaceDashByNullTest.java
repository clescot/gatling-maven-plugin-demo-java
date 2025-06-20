import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplaceDashByNullTest {

    @Test
    public void testReplaceDashByNullWithDash() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String sanitizedString = gatlingReporter.replaceDashByNull("-");
        assertThat(sanitizedString).isNull();
    }

    @Test
    public void testReplaceDashByNullWithNull() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String sanitizedString = gatlingReporter.replaceDashByNull(null);
        assertThat(sanitizedString).isNull();
    }
    @Test
    public void testReplaceDashByNullWithRegularString() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String input = "1234azerty";
        String sanitizedString = gatlingReporter.replaceDashByNull(input);
        assertThat(sanitizedString).isEqualTo(input);
    }

    @Test
    public void testReplaceDashByNullWithEmptyString() {
        // Given
        GatlingReporter gatlingReporter = new GatlingReporter();
        String input = "";
        String sanitizedString = gatlingReporter.replaceDashByNull(input);
        assertThat(sanitizedString).isEqualTo(input);
    }
}
