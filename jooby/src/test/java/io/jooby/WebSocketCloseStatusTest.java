/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Optional;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

public class WebSocketCloseStatusTest {

  @Test
  public void constructorAndGetters() {
    // Standard use
    WebSocketCloseStatus status = new WebSocketCloseStatus(1000, "Normal");
    assertEquals(1000, status.getCode());
    assertEquals("Normal", status.getReason());

    // Null reason
    WebSocketCloseStatus nullReason = new WebSocketCloseStatus(1001, null);
    assertNull(nullReason.getReason());

    // Empty reason (should be treated as null based on the length > 0 check)
    WebSocketCloseStatus emptyReason = new WebSocketCloseStatus(1002, "");
    assertNull(emptyReason.getReason());
  }

  @ParameterizedTest(name = "Code {0} should return {1}")
  @MethodSource("provideStatusCodes")
  public void valueOf(int code, WebSocketCloseStatus expected) {
    Optional<WebSocketCloseStatus> result = WebSocketCloseStatus.valueOf(code);
    if (expected == null) {
      assertFalse(result.isPresent());
    } else {
      assertTrue(result.isPresent());
      assertEquals(expected.getCode(), result.get().getCode());
      assertEquals(expected.getReason(), result.get().getReason());
    }
  }

  private static Stream<Arguments> provideStatusCodes() {
    return Stream.of(
        Arguments.of(-1, WebSocketCloseStatus.NORMAL),
        Arguments.of(1000, WebSocketCloseStatus.NORMAL),
        Arguments.of(1001, WebSocketCloseStatus.GOING_AWAY),
        Arguments.of(1002, WebSocketCloseStatus.PROTOCOL_ERROR),
        Arguments.of(1003, WebSocketCloseStatus.NOT_ACCEPTABLE),
        Arguments.of(1007, WebSocketCloseStatus.BAD_DATA),
        Arguments.of(1008, WebSocketCloseStatus.POLICY_VIOLATION),
        Arguments.of(1009, WebSocketCloseStatus.TOO_BIG_TO_PROCESS),
        Arguments.of(1010, WebSocketCloseStatus.REQUIRED_EXTENSION),
        Arguments.of(1011, WebSocketCloseStatus.SERVER_ERROR),
        Arguments.of(1012, WebSocketCloseStatus.SERVICE_RESTARTED),
        Arguments.of(1013, WebSocketCloseStatus.SERVICE_OVERLOAD),
        // Default case
        Arguments.of(
            1006,
            null), // Note: HARSH_DISCONNECT (1006) is a constant but missing from valueOf switch
        Arguments.of(9999, null));
  }

  @Test
  public void testToString() {
    assertEquals("1000(Normal)", WebSocketCloseStatus.NORMAL.toString());

    WebSocketCloseStatus noReason = new WebSocketCloseStatus(4000, null);
    assertEquals("4000", noReason.toString());
  }

  @Test
  public void checkStaticConstants() {
    // Simple check to ensure constants are initialized as expected
    assertEquals(1000, WebSocketCloseStatus.NORMAL_CODE);
    assertEquals(1001, WebSocketCloseStatus.GOING_AWAY_CODE);
    assertEquals(1002, WebSocketCloseStatus.PROTOCOL_ERROR_CODE);
    assertEquals(1003, WebSocketCloseStatus.NOT_ACCEPTABLE_CODE);
    assertEquals(1006, WebSocketCloseStatus.HARSH_DISCONNECT_CODE);
    assertEquals(1007, WebSocketCloseStatus.BAD_DATA_CODE);
    assertEquals(1008, WebSocketCloseStatus.POLICY_VIOLATION_CODE);
    assertEquals(1009, WebSocketCloseStatus.TOO_BIG_TO_PROCESS_CODE);
    assertEquals(1010, WebSocketCloseStatus.REQUIRED_EXTENSION_CODE);
    assertEquals(1011, WebSocketCloseStatus.SERVER_ERROR_CODE);
    assertEquals(1012, WebSocketCloseStatus.SERVICE_RESTARTED_CODE);
    assertEquals(1013, WebSocketCloseStatus.SERVICE_OVERLOAD_CODE);

    // Verify a few specifically
    assertNotNull(WebSocketCloseStatus.HARSH_DISCONNECT);
    assertEquals(1006, WebSocketCloseStatus.HARSH_DISCONNECT.getCode());
  }
}
