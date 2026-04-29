/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Type;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Reified;

public class JsonDecoderTest {

  @Test
  @DisplayName("Verify decode(String, Class) delegates to decode(String, Type)")
  void testDecodeClassDelegatesToType() {
    JsonDecoder decoder = mock(JsonDecoder.class);
    // Tell Mockito to execute the actual code inside the default method
    when(decoder.decode(any(String.class), any(Class.class))).thenCallRealMethod();

    String json = "{\"name\":\"jooby\"}";
    String expectedResult = "jooby-parsed";

    // Setup the mock behavior for the abstract method it should delegate to
    when(decoder.decode(json, (Type) String.class)).thenReturn(expectedResult);

    // Call the default method
    String result = decoder.decode(json, String.class);

    // Verify the result and the delegation
    assertEquals(expectedResult, result);
    verify(decoder).decode(json, (Type) String.class);
  }

  @Test
  @DisplayName("Verify decode(String, Reified) delegates to decode(String, Type)")
  void testDecodeReifiedDelegatesToType() {
    JsonDecoder decoder = mock(JsonDecoder.class);
    // Tell Mockito to execute the actual code inside the default method
    when(decoder.decode(any(String.class), any(Reified.class))).thenCallRealMethod();

    String json = "[\"a\", \"b\"]";
    List<String> expectedResult = List.of("a", "b");
    Reified<List<String>> reifiedType = Reified.list(String.class);

    // Setup the mock behavior for the abstract method using the Reified Type
    when(decoder.decode(json, reifiedType.getType())).thenReturn(expectedResult);

    // Call the default method
    List<String> result = decoder.decode(json, reifiedType);

    // Verify the result and the delegation
    assertEquals(expectedResult, result);
    verify(decoder).decode(json, reifiedType.getType());
  }
}
