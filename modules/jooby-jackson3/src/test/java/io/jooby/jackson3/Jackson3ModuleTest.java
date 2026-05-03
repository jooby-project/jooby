/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jackson3;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import io.jooby.*;
import io.jooby.internal.jackson3.JacksonJsonCodec;
import io.jooby.json.JsonCodec;
import io.jooby.json.JsonDecoder;
import io.jooby.json.JsonEncoder;
import io.jooby.output.Output;
import io.jooby.output.OutputFactory;
import tools.jackson.core.Version;
import tools.jackson.core.exc.StreamReadException;
import tools.jackson.databind.DatabindException;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.type.TypeFactory;

class Jackson3ModuleTest {

  /**
   * A safely implemented dummy module to ensure Jackson doesn't crash trying to invoke mocked
   * methods during builder.addModule().
   */
  static class DummyModule extends JacksonModule {
    @Override
    public String getModuleName() {
      return "dummy";
    }

    @Override
    public Version version() {
      return Version.unknownVersion();
    }

    @Override
    public void setupModule(SetupContext context) {}
  }

  // --- CONSTRUCTORS & FACTORIES ---

  @Test
  void testConstructorsAndCreate() {
    Jackson3Module module1 = new Jackson3Module();
    assertNotNull(module1);

    ObjectMapper mapper = mock(ObjectMapper.class);
    when(mapper.getTypeFactory()).thenReturn(mock(TypeFactory.class));

    Jackson3Module module2 = new Jackson3Module(mapper);
    assertNotNull(module2);

    Jackson3Module module3 = new Jackson3Module(mapper, MediaType.json);
    assertNotNull(module3);

    JsonMapper createdMapper = Jackson3Module.create();
    assertNotNull(createdMapper);

    DummyModule jModule = new DummyModule();
    JsonMapper createdMapper2 = Jackson3Module.create(jModule);
    assertNotNull(createdMapper2);
  }

  // --- INSTALLATION & BOOTSTRAPPING ---

  @Test
  void testInstall() {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);

    module.install(app);

    verify(app).decoder(MediaType.json, module);
    verify(app).encoder(MediaType.json, module);
    verify(registry).put(ObjectMapper.class, mapper);
    verify(registry).putIfAbsent(eq(JsonCodec.class), any(JacksonJsonCodec.class));
    verify(registry).putIfAbsent(eq(JsonEncoder.class), any(JacksonJsonCodec.class));
    verify(registry).putIfAbsent(eq(JsonDecoder.class), any(JacksonJsonCodec.class));

    verify(app).errorCode(StreamReadException.class, StatusCode.BAD_REQUEST);
    verify(app).errorCode(DatabindException.class, StatusCode.BAD_REQUEST);
    verify(app).onStarting(any(SneakyThrows.Runnable.class));
  }

  @Test
  void testOnStartingWithModules() {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper, MediaType.json);

    module.module(DummyModule.class);

    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);

    DummyModule myModuleInstance = new DummyModule();
    when(app.require(DummyModule.class)).thenReturn(myModuleInstance);

    DummyModule extraModule = new DummyModule();
    when(registry.getOrNull(any(Reified.class))).thenReturn(List.of(extraModule));

    module.install(app);

    ArgumentCaptor<SneakyThrows.Runnable> captor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(captor.capture());

    Runnable onStartingTask = captor.getValue();
    onStartingTask
        .run(); // Executes computeModules, rebuilds mapper, and wires the internal projectionMapper
  }

  @Test
  void testOnStartingWithoutModules() {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper, MediaType.json);

    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);
    when(registry.getOrNull(any(Reified.class))).thenReturn(null);

    module.install(app);

    ArgumentCaptor<SneakyThrows.Runnable> captor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(captor.capture());

    Runnable onStartingTask = captor.getValue();
    onStartingTask.run(); // Covers branch when modules list evaluates to empty
  }

  // --- ENCODING TESTS ---

  @Test
  void testEncodeNormal() {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Context ctx = mock(Context.class);
    OutputFactory factory = mock(OutputFactory.class);
    when(ctx.getOutputFactory()).thenReturn(factory);
    Output output = mock(Output.class);
    when(factory.wrap(any(byte[].class))).thenReturn(output);

    Output result = module.encode(ctx, Map.of("key", "value"));

    assertEquals(output, result);
    verify(ctx).setDefaultResponseType(MediaType.json);
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  void testEncodeProjected() {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Jooby app = mock(Jooby.class);
    ServiceRegistry registry = mock(ServiceRegistry.class);
    when(app.getServices()).thenReturn(registry);
    module.install(app);
    ArgumentCaptor<SneakyThrows.Runnable> captor =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);
    verify(app).onStarting(captor.capture());
    captor.getValue().run(); // Essential for initializing internal projectionMapper

    Context ctx = mock(Context.class);
    OutputFactory factory = mock(OutputFactory.class);
    when(ctx.getOutputFactory()).thenReturn(factory);
    Output output = mock(Output.class);
    when(factory.wrap(any(byte[].class))).thenReturn(output);

    Projected projected = mock(Projected.class);
    Projection projection = mock(Projection.class);
    when(projected.getProjection()).thenReturn(projection);
    when(projected.getValue()).thenReturn(Map.of("key", "value"));

    when(projection.getType()).thenReturn((Class) Map.class);
    when(projection.toView()).thenReturn("test-view");
    when(projection.getChildren()).thenReturn(java.util.Collections.emptyMap());

    // Act 1: Cache miss evaluates supplier
    Output result1 = module.encode(ctx, projected);
    assertEquals(output, result1);

    // Act 2: Cache hit
    Output result2 = module.encode(ctx, projected);
    assertEquals(output, result2);
  }

  // --- DECODING TESTS ---

  @Test
  void testDecodeInMemoryPOJO() throws Exception {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Context ctx = mock(Context.class);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.isInMemory()).thenReturn(true);
    when(body.bytes()).thenReturn("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

    Object result = module.decode(ctx, Map.class);
    assertTrue(result instanceof Map);
    assertEquals("value", ((Map<?, ?>) result).get("key"));
  }

  @Test
  void testDecodeInMemoryJsonNode() throws Exception {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Context ctx = mock(Context.class);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.isInMemory()).thenReturn(true);
    when(body.bytes()).thenReturn("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));

    Object result = module.decode(ctx, JsonNode.class);
    assertTrue(result instanceof JsonNode);
    assertEquals("value", ((JsonNode) result).get("key").asText());
  }

  @Test
  void testDecodeStreamPOJO() throws Exception {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Context ctx = mock(Context.class);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.isInMemory()).thenReturn(false);
    InputStream stream =
        new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
    when(body.stream()).thenReturn(stream);

    Object result = module.decode(ctx, Map.class);
    assertTrue(result instanceof Map);
    assertEquals("value", ((Map<?, ?>) result).get("key"));
  }

  @Test
  void testDecodeStreamJsonNode() throws Exception {
    JsonMapper mapper = JsonMapper.builder().build();
    Jackson3Module module = new Jackson3Module(mapper);

    Context ctx = mock(Context.class);
    Body body = mock(Body.class);
    when(ctx.body()).thenReturn(body);
    when(body.isInMemory()).thenReturn(false);
    InputStream stream =
        new ByteArrayInputStream("{\"key\":\"value\"}".getBytes(StandardCharsets.UTF_8));
    when(body.stream()).thenReturn(stream);

    Object result = module.decode(ctx, JsonNode.class);
    assertTrue(result instanceof JsonNode);
    assertEquals("value", ((JsonNode) result).get("key").asText());
  }
}
