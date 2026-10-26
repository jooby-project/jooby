/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.StatusCode;
import io.jooby.value.Value;
import io.jooby.value.ValueFactory;

public class AssetHandlerTest {

  private static final Instant LAST_MODIFIED = Instant.parse("2026-01-02T03:04:05Z");

  private Asset asset;

  private Context ctx;

  @BeforeEach
  void setUp() {
    asset = mock(Asset.class);
    when(asset.getEtag()).thenReturn("W/\"abc\"");
    when(asset.getLastModified()).thenReturn(LAST_MODIFIED.toEpochMilli());
    when(asset.getSize()).thenReturn(5L);
    when(asset.getContentType()).thenReturn(MediaType.text);
    when(asset.stream())
        .thenReturn(new ByteArrayInputStream("hello".getBytes(StandardCharsets.UTF_8)));

    ctx = mock(Context.class, RETURNS_DEEP_STUBS);
    when(ctx.path((String) any()).value("index.html")).thenReturn("a.txt");
  }

  @ParameterizedTest(name = "If-None-Match: {0}, If-Modified-Since: {1} -> 304: {2}")
  @CsvSource(
      delimiter = '|',
      nullValues = "none",
      value = {
        "W/\"abc\"                    | none          | true",
        "\"abc\"                      | none          | true",
        "\"x\", W/\"abc\"             | none          | true",
        "\"x\",\"abc\"                | none          | true",
        "*                            | none          | true",
        "\"x\"                        | none          | false",
        "\"x\"                        | last-modified | false",
        "none                         | last-modified | true",
        "none                         | yesterday     | false",
      })
  void conditionalRequest(
      @Nullable String ifNoneMatch, @Nullable String ifModifiedSince, boolean notModified)
      throws Exception {
    header("If-None-Match", ifNoneMatch);
    header(
        "If-Modified-Since",
        "last-modified".equals(ifModifiedSince)
            ? Context.RFC1123.format(LAST_MODIFIED.atOffset(ZoneOffset.UTC))
            : ifModifiedSince);

    new AssetHandler(path -> asset).apply(ctx);

    if (notModified) {
      verify(ctx).send(StatusCode.NOT_MODIFIED);
    } else {
      verify(ctx, never()).send(StatusCode.NOT_MODIFIED);
      verify(ctx).send(any(java.io.InputStream.class));
    }
  }

  private void header(String name, @Nullable String value) {
    ValueFactory factory = new ValueFactory();
    Value header = value == null ? Value.missing(factory, name) : Value.value(factory, name, value);
    when(ctx.header(name)).thenReturn(header);
  }
}
