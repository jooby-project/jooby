/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.exception.MissingValueException;
import io.jooby.value.Value;

public class BodyTest {

  @Test
  @DisplayName("Verify value(Charset) decoding branches")
  void testValueCharset() {
    Body body = mock(Body.class);
    when(body.value(any(java.nio.charset.Charset.class))).thenCallRealMethod();

    // Branch 1: Missing value (empty byte array)
    when(body.bytes()).thenReturn(new byte[0]);
    assertThrows(MissingValueException.class, () -> body.value(StandardCharsets.UTF_8));

    // Branch 2: Successful decode
    String testContent = "jooby-body";
    when(body.bytes()).thenReturn(testContent.getBytes(StandardCharsets.UTF_8));
    assertEquals(testContent, body.value(StandardCharsets.UTF_8));
  }

  @Test
  @DisplayName("Verify size() returns 1 as defined by default method")
  void testSize() {
    Body body = mock(Body.class);
    when(body.size()).thenCallRealMethod();
    assertEquals(1, body.size());
  }

  @Test
  @DisplayName("Verify get(int) delegates to get(String)")
  void testGetInt() {
    Body body = mock(Body.class);
    Value expectedValue = mock(Value.class);

    when(body.get("5")).thenReturn(expectedValue);
    when(body.get(5)).thenCallRealMethod();

    assertEquals(expectedValue, body.get(5));
    verify(body).get("5");
  }

  @Test
  @DisplayName("Verify iterator wraps the body instance")
  void testIterator() {
    Body body = mock(Body.class);
    when(body.iterator()).thenCallRealMethod();

    Iterator<Value> iterator = body.iterator();

    assertTrue(iterator.hasNext());
    assertEquals(body, iterator.next());
    assertFalse(iterator.hasNext());
  }

  @Test
  @DisplayName("Verify toList(Class) delegates to to(Type) via Reified")
  void testToListClass() {
    Body body = mock(Body.class);
    List<String> expectedList = List.of("a", "b");

    // We capture the Reified Type delegation
    when(body.to(any(Type.class))).thenReturn(expectedList);
    when(body.toList(String.class)).thenCallRealMethod();

    assertEquals(expectedList, body.toList(String.class));
  }

  @Test
  @DisplayName("Verify string-based toList() and toSet() collections")
  void testStringCollections() {
    Body body = mock(Body.class);
    when(body.value()).thenReturn("test-value");
    when(body.toList()).thenCallRealMethod();
    when(body.toSet()).thenCallRealMethod();

    assertEquals(List.of("test-value"), body.toList());
    assertEquals(Set.of("test-value"), body.toSet());
  }

  @Test
  @DisplayName("Verify class-based conversions delegate to type-based conversions")
  void testClassConversions() {
    Body body = mock(Body.class);

    when(body.to((Type) Integer.class)).thenReturn(100);
    when(body.to(Integer.class)).thenCallRealMethod();
    assertEquals(100, body.to(Integer.class));

    when(body.toNullable((Type) Long.class)).thenReturn(200L);
    when(body.toNullable(Long.class)).thenCallRealMethod();
    assertEquals(200L, body.toNullable(Long.class));
  }

  @Test
  @DisplayName("Verify static factory methods route to correct internal implementations")
  void testStaticFactories() {
    Context ctx = mock(Context.class);

    // Using getClass().getSimpleName() avoids restricted visibility issues
    // if internal classes are package-private while ensuring the correct factory was invoked.

    Body emptyBody = Body.empty(ctx);
    assertEquals("ByteArrayBody", emptyBody.getClass().getSimpleName());

    Body bytesBody = Body.of(ctx, new byte[] {1, 2, 3});
    assertEquals("ByteArrayBody", bytesBody.getClass().getSimpleName());

    InputStream stream = mock(InputStream.class);
    Body streamBody = Body.of(ctx, stream, 1024L);
    assertEquals("InputStreamBody", streamBody.getClass().getSimpleName());

    Path file = Paths.get("dummy.txt");
    Body fileBody = Body.of(ctx, file);
    assertEquals("FileBody", fileBody.getClass().getSimpleName());
  }
}
