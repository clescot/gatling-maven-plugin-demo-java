import org.junit.Test;
import org.junit.jupiter.api.Assertions;

import static org.assertj.core.api.Assertions.assertThat;

public class ConvertCamelCaseToSnakeTest {

    @Test
    public void testNominalCase() {
        // Given
        String input = "camelCaseString";
        String expectedOutput = "camel_case_string";
        GatlingReporter gatlingReporter = new GatlingReporter();
        // When
        String actualOutput = gatlingReporter.convertCamelCaseToSnake(input);

        // Then
        assertThat(actualOutput).isEqualTo(expectedOutput);
    }

    @Test
    public void testNullValue() {
        // Given
        String expectedOutput = "camel_case_string";
        GatlingReporter gatlingReporter = new GatlingReporter();
        Assertions.assertThrows(NullPointerException.class, () -> gatlingReporter.convertCamelCaseToSnake(null));
    }



}