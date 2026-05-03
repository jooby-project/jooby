/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.PropertyWriter;
import io.jooby.Projection;

class JacksonProjectionFilterTest {

  private JsonGenerator jgen;
  private SerializerProvider provider;
  private PropertyWriter writer;
  private Projection<?> rootProjection;

  @BeforeEach
  void setUp() {
    jgen = mock(JsonGenerator.class);
    provider = mock(SerializerProvider.class);
    writer = mock(PropertyWriter.class);
    rootProjection = mock(Projection.class);
  }

  @Test
  void testSerializeAsField_MapBypass() throws Exception {
    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    Map<String, String> pojo = new HashMap<>();

    filter.serializeAsField(pojo, jgen, provider, writer);

    // Map should immediately serialize and bypass projection logic
    verify(writer).serializeAsField(pojo, jgen, provider);
    verify(jgen, never()).getOutputContext();
  }

  @Test
  void testSerializeAsField_NullContext_FieldAllowed() throws Exception {
    when(jgen.getOutputContext()).thenReturn(null);
    when(writer.getName()).thenReturn("allowedField");

    // Root projection with an allowed child
    Projection<?> child = mock(Projection.class);
    when(rootProjection.getChildren()).thenReturn(Map.of("allowedField", child));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    verify(writer).serializeAsField(any(), any(), any());
  }

  @Test
  void testSerializeAsField_NullContext_FieldBlocked() throws Exception {
    when(jgen.getOutputContext()).thenReturn(null);
    when(writer.getName()).thenReturn("blockedField");

    // Root projection with only a different child
    Projection<?> child = mock(Projection.class);
    when(rootProjection.getChildren()).thenReturn(Map.of("allowedField", child));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    // Should NOT serialize
    verify(writer, never()).serializeAsField(any(), any(), any());
  }

  @Test
  void testSerializeAsField_NullContext_WildcardRoot() throws Exception {
    when(jgen.getOutputContext()).thenReturn(null);
    when(writer.getName()).thenReturn("anyField");

    // Root projection with EMPTY children (acts as wildcard)
    when(rootProjection.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    verify(writer).serializeAsField(any(), any(), any());
  }

  @Test
  void testResolveNode_FullValidPath() throws Exception {
    // Context hierarchy: rootCtx -> userCtx -> addressCtx -> currentOutputCtx
    JsonStreamContext rootCtx = createMockContext(null, false, true, null);
    JsonStreamContext userCtx = createMockContext("user", true, false, rootCtx);
    JsonStreamContext addressCtx = createMockContext("address", true, false, userCtx);
    JsonStreamContext currentOutputCtx = createMockContext(null, true, false, addressCtx);

    when(jgen.getOutputContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("city");

    Projection<?> userProj = mock(Projection.class);
    Projection<?> addressProj = mock(Projection.class);
    Projection<?> cityProj = mock(Projection.class);

    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    when(userProj.getChildren()).thenReturn(Map.of("address", addressProj));
    when(addressProj.getChildren()).thenReturn(Map.of("city", cityProj));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    verify(writer).serializeAsField(any(), any(), any());
  }

  @Test
  void testResolveNode_IntermediateWildcard() throws Exception {
    // Context hierarchy: rootCtx -> userCtx -> addressCtx -> currentOutputCtx
    JsonStreamContext rootCtx = createMockContext(null, false, true, null);
    JsonStreamContext userCtx = createMockContext("user", true, false, rootCtx);
    JsonStreamContext addressCtx = createMockContext("address", true, false, userCtx);
    JsonStreamContext currentOutputCtx = createMockContext(null, true, false, addressCtx);

    when(jgen.getOutputContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("zipcode");

    Projection<?> userProj = mock(Projection.class);
    Projection<?> addressProj = mock(Projection.class);

    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    // User has address, but address has NO children (wildcard)
    when(userProj.getChildren()).thenReturn(Map.of("address", addressProj));
    when(addressProj.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    // Because 'address' is a wildcard node, 'zipcode' is allowed automatically
    verify(writer).serializeAsField(any(), any(), any());
  }

  @Test
  void testResolveNode_PathOutsideTree_ThrowsNodeNullInsideLoop() throws Exception {
    // Path: ["unknown", "something"] to trigger the `if (node == null)` inside the loop
    JsonStreamContext rootCtx = createMockContext(null, false, true, null);
    JsonStreamContext unknownCtx = createMockContext("unknown", true, false, rootCtx);
    JsonStreamContext somethingCtx = createMockContext("something", true, false, unknownCtx);
    JsonStreamContext currentOutputCtx = createMockContext(null, true, false, somethingCtx);

    when(jgen.getOutputContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("any");

    // Root doesn't have "unknown"
    when(rootProjection.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    // Current resolves to null, so serialization is skipped
    verify(writer, never()).serializeAsField(any(), any(), any());
  }

  @Test
  void testResolveNode_ArrayAndNullNameIgnored() throws Exception {
    // Context hierarchy tests the array bypass and null name bypass logic:
    // rootCtx -> userCtx -> arrayCtx (inObject=false) -> nullNameCtx (null name) ->
    // currentOutputCtx
    JsonStreamContext rootCtx = createMockContext(null, false, true, null);
    JsonStreamContext userCtx = createMockContext("user", true, false, rootCtx);
    JsonStreamContext arrayCtx =
        createMockContext("ignored", false, false, userCtx); // inObject = false
    JsonStreamContext nullNameCtx = createMockContext(null, true, false, arrayCtx); // null name
    JsonStreamContext currentOutputCtx = createMockContext(null, true, false, nullNameCtx);

    when(jgen.getOutputContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("profile");

    Projection<?> userProj = mock(Projection.class);
    Projection<?> profileProj = mock(Projection.class);

    // Root only needs "user" because the array and null name contexts are ignored
    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    when(userProj.getChildren()).thenReturn(Map.of("profile", profileProj));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsField(new Object(), jgen, provider, writer);

    verify(writer).serializeAsField(any(), any(), any());
  }

  /** Helper method to create Mocked JsonStreamContexts concisely. */
  private JsonStreamContext createMockContext(
      String currentName, boolean inObject, boolean inRoot, JsonStreamContext parent) {
    JsonStreamContext ctx = mock(JsonStreamContext.class);
    when(ctx.getCurrentName()).thenReturn(currentName);
    when(ctx.inObject()).thenReturn(inObject);
    when(ctx.inRoot()).thenReturn(inRoot);
    when(ctx.getParent()).thenReturn(parent);
    return ctx;
  }
}
