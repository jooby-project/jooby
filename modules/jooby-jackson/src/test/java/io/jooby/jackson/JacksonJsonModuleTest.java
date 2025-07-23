/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.jooby.Body;
import io.jooby.Context;
import io.jooby.MediaType;
import io.jooby.output.OutputFactory;
import io.jooby.output.OutputOptions;

public class JacksonJsonModuleTest {

  @Test
  public void renderJson() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.getOutputFactory()).thenReturn(OutputFactory.create(OutputOptions.small()));

    JacksonModule jackson = new JacksonModule(new ObjectMapper());

    var buffer = jackson.encode(ctx, mapOf("k", "v"));
    assertEquals("{\"k\":\"v\"}", StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString());

    verify(ctx).setDefaultResponseType(MediaType.json);
  }

  @Test
  public void parseJson() throws Exception {
    byte[] bytes = "{\"k\":\"v\"}".getBytes(StandardCharsets.UTF_8);
    Body body = mock(Body.class);
    when(body.isInMemory()).thenReturn(true);
    when(body.bytes()).thenReturn(bytes);

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(body);

    JacksonModule jackson = new JacksonModule(new ObjectMapper());

    Map<String, String> result = (Map<String, String>) jackson.decode(ctx, Map.class);
    assertEquals(mapOf("k", "v"), result);
  }

  @Test
  public void renderXml() throws Exception {
    Context ctx = mock(Context.class);
    when(ctx.getOutputFactory()).thenReturn(OutputFactory.create(OutputOptions.small()));

    JacksonModule jackson = new JacksonModule(new XmlMapper());

    var buffer = jackson.encode(ctx, mapOf("k", "v"));
    assertEquals(
        "<HashMap><k>v</k></HashMap>",
        StandardCharsets.UTF_8.decode(buffer.asByteBuffer()).toString());

    verify(ctx).setDefaultResponseType(MediaType.xml);
  }

  @Test
  public void parseXml() throws Exception {
    byte[] bytes = "<HashMap><k>v</k></HashMap>".getBytes(StandardCharsets.UTF_8);
    Body body = mock(Body.class);
    when(body.isInMemory()).thenReturn(true);
    when(body.bytes()).thenReturn(bytes);

    Context ctx = mock(Context.class);
    when(ctx.body()).thenReturn(body);

    JacksonModule jackson = new JacksonModule(new XmlMapper());

    Map<String, String> result = (Map<String, String>) jackson.decode(ctx, Map.class);
    assertEquals(mapOf("k", "v"), result);
  }

  private Map<String, String> mapOf(String... values) {
    Map<String, String> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return hash;
  }
}
