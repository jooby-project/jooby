/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.channels.ReadableByteChannel;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.Body;
import io.jooby.Context;
import io.jooby.FileUpload;
import io.jooby.Formdata;
import io.jooby.QueryString;
import io.jooby.value.ConversionHint;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class ValidationContextTest {

  private MockedStatic<BeanValidator> beanValidatorMock;

  @BeforeEach
  void setUp() {
    // Globally mock BeanValidator so that its static apply() returns the passed object
    // transparently
    beanValidatorMock = mockStatic(BeanValidator.class);
    beanValidatorMock
        .when(() -> BeanValidator.apply(any(Context.class), any()))
        .thenAnswer(invocation -> invocation.getArgument(1));
  }

  @AfterEach
  void tearDown() {
    beanValidatorMock.close();
  }

  @Test
  @DisplayName("Verify ValidatedValue logic for paths and headers")
  void testValidatedValue() {
    Context ctx = mock(Context.class);
    ValueFactory valueFactory = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(valueFactory);

    Value pathValue = mock(Value.class);
    when(ctx.path()).thenReturn(pathValue);

    ValidationContext valCtx = new ValidationContext(ctx);
    Value path = valCtx.path();

    when(valueFactory.convert(eq(String.class), any(Value.class), eq(ConversionHint.Empty)))
        .thenReturn("validated-string");
    when(pathValue.toList(String.class)).thenReturn(List.of("a"));
    when(pathValue.toSet(String.class)).thenReturn(Set.of("a"));

    assertEquals("validated-string", path.to(String.class));
    assertEquals("validated-string", path.toNullable(String.class));
    assertEquals(List.of("a"), path.toList(String.class));
    assertEquals(Set.of("a"), path.toSet(String.class));

    // Verify header returns wrapped value as well
    Value headerValue = mock(Value.class);
    when(ctx.header()).thenReturn(headerValue);
    Value header = valCtx.header();

    when(valueFactory.convert(eq(Integer.class), any(Value.class), eq(ConversionHint.Empty)))
        .thenReturn(123);
    assertEquals(123, header.to(Integer.class));
  }

  @Test
  @DisplayName("Verify ValidatedBody wraps Body properties and conversion methods")
  void testValidatedBody() {
    Context ctx = mock(Context.class);
    Body bodyMock = mock(Body.class);
    when(ctx.body()).thenReturn(bodyMock);

    ValidationContext valCtx = new ValidationContext(ctx);
    Body body = valCtx.body();

    // 1. Pass-through Property checks
    byte[] bytes = new byte[0];
    when(bodyMock.bytes()).thenReturn(bytes);
    assertSame(bytes, body.bytes());

    when(bodyMock.isInMemory()).thenReturn(true);
    assertTrue(body.isInMemory());

    when(bodyMock.getSize()).thenReturn(100L);
    assertEquals(100L, body.getSize());

    ReadableByteChannel channel = mock(ReadableByteChannel.class);
    when(bodyMock.channel()).thenReturn(channel);
    assertSame(channel, body.channel());

    InputStream stream = mock(InputStream.class);
    when(bodyMock.stream()).thenReturn(stream);
    assertSame(stream, body.stream());

    // 2. Conversion and Shortcut Checks
    when(bodyMock.toNullable((Type) String.class)).thenReturn("type-string");
    assertEquals("type-string", body.to((Type) String.class));
    assertEquals("type-string", body.toNullable((Type) String.class));

    // Context shortcut for Type
    assertEquals("type-string", valCtx.body((Type) String.class));

    when(bodyMock.toNullable(String.class)).thenReturn("class-string");
    assertEquals("class-string", body.to(String.class));

    // Context shortcut for Class
    assertEquals("class-string", valCtx.body(String.class));
  }

  @Test
  @DisplayName("Verify ValidatedQueryString wraps QueryString and conversion methods")
  void testValidatedQueryString() {
    Context ctx = mock(Context.class);
    QueryString qsMock = mock(QueryString.class);
    when(ctx.query()).thenReturn(qsMock);

    ValueFactory valueFactory = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(valueFactory);

    ValidationContext valCtx = new ValidationContext(ctx);
    QueryString qs = valCtx.query();

    when(qsMock.queryString()).thenReturn("?a=b");
    assertEquals("?a=b", qs.queryString());

    when(valueFactory.convert(eq(String.class), any(Value.class), eq(ConversionHint.Empty)))
        .thenReturn("qs-validated");

    assertEquals("qs-validated", qs.toEmpty(String.class));
    assertEquals("qs-validated", valCtx.query(String.class));
  }

  @Test
  @DisplayName("Verify ValidatedFormdata passes through mutation correctly")
  void testValidatedFormdata() {
    Context ctx = mock(Context.class);
    Formdata formMock = mock(Formdata.class);
    when(ctx.form()).thenReturn(formMock);

    ValidationContext valCtx = new ValidationContext(ctx);
    Formdata form = valCtx.form();

    // 1. Mutation Checks
    Value valMock = mock(Value.class);
    form.put("path1", valMock);
    verify(formMock).put("path1", valMock);

    form.put("path2", "string-val");
    verify(formMock).put("path2", "string-val");

    Collection<String> coll = List.of("a");
    form.put("path3", coll);
    verify(formMock).put("path3", coll);

    FileUpload fileMock = mock(FileUpload.class);
    form.put("file1", fileMock);
    verify(formMock).put("file1", fileMock);

    // 2. Fetch Checks
    List<FileUpload> files = List.of(fileMock);
    when(formMock.files()).thenReturn(files);
    assertSame(files, form.files());

    when(formMock.files("file1")).thenReturn(files);
    assertSame(files, form.files("file1"));

    when(formMock.file("file1")).thenReturn(fileMock);
    assertSame(fileMock, form.file("file1"));

    // 3. Conversion and Shortcut Checks
    ValueFactory valueFactory = mock(ValueFactory.class);
    when(ctx.getValueFactory()).thenReturn(valueFactory);
    when(valueFactory.convert(eq(String.class), any(Value.class), eq(ConversionHint.Empty)))
        .thenReturn("form-validated");

    assertEquals("form-validated", valCtx.form(String.class));
  }
}
