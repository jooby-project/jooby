package io.jooby.problem;

import io.jooby.StatusCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static io.jooby.StatusCode.BAD_REQUEST;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class HttpProblemTest {

  @Test
  void problemEmptyBuilder_shouldThrowException() {
    var ex = assertThrows(RuntimeException.class, () ->
        HttpProblem.builder().build()
    );
    assertEquals("The problem 'title' should be specified", ex.getMessage());
  }

  @Test
  void problemWithoutTitle_shouldThrowException() {
    var ex = assertThrows(RuntimeException.class, () ->
        HttpProblem.builder().detail("detail").build()
    );
    assertEquals("The problem 'title' should be specified", ex.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "   "})
  @NullSource
  void problemWithEmptyTitle_shouldThrowException(String value) {
    var ex = assertThrows(RuntimeException.class, () ->
        HttpProblem.builder().title(value).build()
    );
    assertEquals("The problem 'title' should be specified", ex.getMessage());
  }

  @Test
  void problemWithoutStatus_shouldThrowException() {
    var ex = assertThrows(RuntimeException.class, () ->
        HttpProblem.builder().title("title").build()
    );

    assertEquals("The problem 'status' should be specified", ex.getMessage());
  }

  @Test
  void problemWithIllegalStatusCode_shouldThrowException() {
    var ex = assertThrows(RuntimeException.class, () ->
        HttpProblem.builder().title("title").status(StatusCode.ACCEPTED).build()
    );

    assertEquals("Illegal status code " + StatusCode.ACCEPTED.value() + ". " +
                 "Problem details designed to serve 4xx and 5xx status codes", ex.getMessage());
  }

  @Test
  void valueOfStatus() {
    var actual = HttpProblem.valueOf(BAD_REQUEST);
    var expected = HttpProblem.builder()
        .title(BAD_REQUEST.reason())
        .status(BAD_REQUEST)
        .build();

    assertThat(expected)
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(actual);
  }

  @Test
  void valueOfStatusAndTitle() {
    var actual = HttpProblem.valueOf(BAD_REQUEST, "Title");
    var expected = HttpProblem.builder()
        .title("Title")
        .status(BAD_REQUEST)
        .build();

    assertThat(expected)
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(actual);
  }

  @Test
  void valueOfStatusTitleAndDetail() {
    var actual = HttpProblem.valueOf(BAD_REQUEST, "Title", "Detail");
    var expected = HttpProblem.builder()
        .title("Title")
        .status(BAD_REQUEST)
        .detail("Detail")
        .build();

    assertThat(expected)
        .usingRecursiveComparison()
        .ignoringFields("timestamp")
        .isEqualTo(actual);
  }
}
