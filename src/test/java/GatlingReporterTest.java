import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

public class GatlingReporterTest {

    @Test
    public void testConvertCamelCaseToSnakeNominalCase() {
        // Given
        String input = "camelCaseString";
        String expectedOutput = "camel_case_string";
        GatlingReporter gatlingReporter = new GatlingReporter();
        // When
        String actualOutput = gatlingReporter.convertCamelCaseToSnakeRegex(input);

        // Then
        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void testConvertCamelCaseToSnakeNullValue() {
        // Given
        String expectedOutput = "camel_case_string";
        GatlingReporter gatlingReporter = new GatlingReporter();
        Assertions.assertThrows(NullPointerException.class, () -> gatlingReporter.convertCamelCaseToSnakeRegex(null));
    }

}