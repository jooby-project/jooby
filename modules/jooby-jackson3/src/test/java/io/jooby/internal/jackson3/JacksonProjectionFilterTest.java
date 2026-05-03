/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.jackson3;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Projection;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.AnyGetterWriter;
import tools.jackson.databind.ser.PropertyWriter;

class JacksonProjectionFilterTest {

  private JsonGenerator gen;
  private SerializationContext provider;
  private PropertyWriter writer;
  private Projection<?> rootProjection;

  @BeforeEach
  void setUp() {
    gen = mock(JsonGenerator.class);
    provider = mock(SerializationContext.class);
    writer = mock(PropertyWriter.class);
    rootProjection = mock(Projection.class);
  }

  // --- MAP BYPASS TEST ---

  @Test
  void testSerializeAsProperty_MapBypass() throws Exception {
    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    Map<String, String> pojo = new HashMap<>();

    filter.serializeAsProperty(pojo, gen, provider, writer);

    // Map should immediately serialize and bypass all projection and context logic
    verify(writer).serializeAsProperty(pojo, gen, provider);
    verify(gen, never()).streamWriteContext();
  }

  // --- OMIT & ANYGETTER TESTS (WHEN INCLUDE = FALSE) ---

  @Test
  void testSerializeAsProperty_NotIncluded_CannotOmit() throws Exception {
    when(writer.getName()).thenReturn("blockedField");

    // FIX: Extract mock creation before thenReturn to avoid unfinished stubbing
    TokenStreamContext mockCtx = createMockContext(null, true, null);
    when(gen.streamWriteContext()).thenReturn(mockCtx);

    when(gen.canOmitProperties()).thenReturn(false);

    // Root projection with an unrelated child (so "blockedField" evaluates to false)
    Projection<?> child = mock(Projection.class);
    when(rootProjection.getChildren()).thenReturn(Map.of("allowedField", child));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    Object pojo = new Object();
    filter.serializeAsProperty(pojo, gen, provider, writer);

    // Because it's blocked AND we cannot omit properties, it serializes as omitted
    verify(writer).serializeAsOmittedProperty(pojo, gen, provider);
    verify(writer, never()).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testSerializeAsProperty_NotIncluded_CanOmit_AnyGetterWriter() throws Exception {
    AnyGetterWriter anyGetterWriter = mock(AnyGetterWriter.class);
    when(anyGetterWriter.getName()).thenReturn("blockedField");

    // FIX: Extract mock creation before thenReturn
    TokenStreamContext mockCtx = createMockContext(null, true, null);
    when(gen.streamWriteContext()).thenReturn(mockCtx);

    when(gen.canOmitProperties()).thenReturn(true);

    // Root projection with an unrelated child
    Projection<?> child = mock(Projection.class);
    when(rootProjection.getChildren()).thenReturn(Map.of("allowedField", child));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    Object pojo = new Object();
    filter.serializeAsProperty(pojo, gen, provider, anyGetterWriter);

    // Because it's blocked, can omit, and IS an AnyGetterWriter
    verify(anyGetterWriter).getAndFilter(eq(pojo), eq(gen), eq(provider), eq(filter));
    verify(anyGetterWriter, never()).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testSerializeAsProperty_NotIncluded_CanOmit_NormalWriter() throws Exception {
    when(writer.getName()).thenReturn("blockedField");

    // FIX: Extract mock creation before thenReturn
    TokenStreamContext mockCtx = createMockContext(null, true, null);
    when(gen.streamWriteContext()).thenReturn(mockCtx);

    when(gen.canOmitProperties()).thenReturn(true);

    // Root projection with an unrelated child
    Projection<?> child = mock(Projection.class);
    when(rootProjection.getChildren()).thenReturn(Map.of("allowedField", child));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    Object pojo = new Object();
    filter.serializeAsProperty(pojo, gen, provider, writer);

    // Because it's blocked, can omit, and is a normal writer, it does NOTHING
    verify(writer, never()).serializeAsProperty(any(), any(), any());
    verify(writer, never()).serializeAsOmittedProperty(any(), any(), any());
  }

  // --- INCLUDE LOGIC TESTS ---

  @Test
  void testInclude_NullProjection() throws Exception {
    when(writer.getName()).thenReturn("anyField");

    // Null projection should allow everything
    JacksonProjectionFilter filter = new JacksonProjectionFilter(null);
    filter.serializeAsProperty(new Object(), gen, provider, writer);

    verify(writer).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testInclude_EmptyProjectionChildren() throws Exception {
    when(writer.getName()).thenReturn("anyField");
    when(rootProjection.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsProperty(new Object(), gen, provider, writer);

    verify(writer).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testInclude_PathFullyConsumedAndMatched() throws Exception {
    // Path: ["user", "name"]
    TokenStreamContext rootCtx = createMockContext(null, true, null);
    TokenStreamContext userCtx = createMockContext("user", false, rootCtx);
    TokenStreamContext currentOutputCtx =
        createMockContext(null, false, userCtx); // writer handles "name"

    when(gen.streamWriteContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("name");

    Projection<?> userProj = mock(Projection.class);
    Projection<?> nameProj = mock(Projection.class);

    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    // userProj has children, so the loop continues
    when(userProj.getChildren()).thenReturn(Map.of("name", nameProj));
    // nameProj has children (not empty), so the loop completes successfully and returns true
    when(nameProj.getChildren()).thenReturn(Map.of("subfield", mock(Projection.class)));

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsProperty(new Object(), gen, provider, writer);

    verify(writer).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testInclude_IntermediateDeepWildcard() throws Exception {
    // Path: ["user", "address", "zipcode"]
    TokenStreamContext rootCtx = createMockContext(null, true, null);
    TokenStreamContext userCtx = createMockContext("user", false, rootCtx);
    TokenStreamContext addressCtx = createMockContext("address", false, userCtx);
    TokenStreamContext currentOutputCtx = createMockContext(null, false, addressCtx);

    when(gen.streamWriteContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("zipcode");

    Projection<?> userProj = mock(Projection.class);
    Projection<?> addressProj = mock(Projection.class);

    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    when(userProj.getChildren()).thenReturn(Map.of("address", addressProj));
    // Address has no explicit children, so it acts as a deep wildcard!
    when(addressProj.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsProperty(new Object(), gen, provider, writer);

    // Should include because 'address' acted as a wildcard for 'zipcode'
    verify(writer).serializeAsProperty(any(), any(), any());
  }

  @Test
  void testInclude_SkipsArrayContexts() throws Exception {
    // Path mapping tests the array bypass:
    // rootCtx -> userCtx -> arrayCtx (currentName = null) -> currentOutputCtx
    TokenStreamContext rootCtx = createMockContext(null, true, null);
    TokenStreamContext userCtx = createMockContext("user", false, rootCtx);
    TokenStreamContext arrayCtx =
        createMockContext(null, false, userCtx); // Simulates array (no name)
    TokenStreamContext currentOutputCtx = createMockContext(null, false, arrayCtx);

    when(gen.streamWriteContext()).thenReturn(currentOutputCtx);
    when(writer.getName()).thenReturn("profile"); // final segment

    Projection<?> userProj = mock(Projection.class);
    Projection<?> profileProj = mock(Projection.class);

    when(rootProjection.getChildren()).thenReturn(Map.of("user", userProj));
    // user -> profile directly. The array context in the middle is seamlessly ignored.
    when(userProj.getChildren()).thenReturn(Map.of("profile", profileProj));
    when(profileProj.getChildren()).thenReturn(Collections.emptyMap());

    JacksonProjectionFilter filter = new JacksonProjectionFilter(rootProjection);
    filter.serializeAsProperty(new Object(), gen, provider, writer);

    verify(writer).serializeAsProperty(any(), any(), any());
  }

  /** Helper method to construct Mocked TokenStreamContext instances */
  private TokenStreamContext createMockContext(
      String currentName, boolean inRoot, TokenStreamContext parent) {
    TokenStreamContext ctx = mock(TokenStreamContext.class);
    when(ctx.currentName()).thenReturn(currentName);
    when(ctx.inRoot()).thenReturn(inRoot);
    when(ctx.getParent()).thenReturn(parent);
    return ctx;
  }
}
