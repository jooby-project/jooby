/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import io.jooby.*;
import io.jooby.exception.StartupException;
import io.jooby.internal.mcp.McpServerConfig;
import io.jooby.mcp.instrumentation.OtelMcpTracing;
import io.modelcontextprotocol.common.McpTransportContext;
import io.modelcontextprotocol.json.McpJsonMapper;
import io.modelcontextprotocol.server.McpTransportContextExtractor;
import io.modelcontextprotocol.spec.McpSchema;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({"unchecked", "rawtypes"})
class McpModuleTest {

  @Mock Jooby app;
  @Mock ServiceRegistry registry;
  @Mock Config config;
  @Mock McpJsonMapper jsonMapper;
  @Mock McpService service1;
  @Mock McpService service2;

  @Mock ServiceRegistry.MultiBinder statelessList;
  @Mock ServiceRegistry.MultiBinder syncList;
  @Mock ServiceRegistry.MultiBinder configList;

  @BeforeEach
  void setup() {
    lenient().when(app.getServices()).thenReturn(registry);
    lenient().when(app.getConfig()).thenReturn(config);

    // Provide default values to prevent SDK IllegalArgumentExceptions on null name/version
    lenient().when(app.getName()).thenReturn("test-app");
    lenient().when(app.getVersion()).thenReturn("1.0.0");

    lenient().when(registry.require(McpJsonMapper.class)).thenReturn(jsonMapper);

    lenient()
        .when(registry.listOf(io.modelcontextprotocol.server.McpStatelessSyncServer.class))
        .thenReturn(statelessList);
    lenient()
        .when(registry.listOf(io.modelcontextprotocol.server.McpSyncServer.class))
        .thenReturn(syncList);
    lenient().when(registry.listOf(McpServerConfig.class)).thenReturn(configList);

    // Mock Jooby route builders invoked by the transports
    Route route = mock(Route.class);
    lenient().when(route.produces(any(MediaType.class))).thenReturn(route);
    lenient().when(route.produces(any(MediaType.class))).thenReturn(route);
    lenient().when(app.head(anyString(), any())).thenReturn(route);
    lenient().when(app.get(anyString(), any())).thenReturn(route);
    lenient().when(app.post(anyString(), any())).thenReturn(route);
    lenient().when(app.sse(anyString(), any())).thenReturn(route);
    lenient().when(app.ws(anyString(), any())).thenReturn(route);

    lenient().when(service1.serverKey()).thenReturn("default");
    lenient().when(service2.serverKey()).thenReturn("custom");
  }

  /**
   * Helper method to intercept the MCP SDK's internal ServiceLoader mechanism. Without this,
   * McpServer.build() throws a ServiceConfigurationError looking for Jackson/JSON modules that are
   * not loaded in the isolated test classpath.
   */
  private void installSafely(McpModule module) {
    try (MockedStatic<io.modelcontextprotocol.json.McpJsonDefaults> defaults =
        mockStatic(io.modelcontextprotocol.json.McpJsonDefaults.class)) {
      defaults
          .when(() -> io.modelcontextprotocol.json.McpJsonDefaults.getMapper())
          .thenReturn(jsonMapper);
      defaults
          .when(() -> io.modelcontextprotocol.json.McpJsonDefaults.getSchemaValidator())
          .thenAnswer(inv -> mock(inv.getMethod().getReturnType()));

      module.install(app);
    }
  }

  // --- ENUM TESTS ---

  @Test
  void testTransportEnum() {
    assertEquals(McpModule.Transport.SSE, McpModule.Transport.of("sse"));
    assertEquals(McpModule.Transport.STREAMABLE_HTTP, McpModule.Transport.of("streamable-http"));
    assertEquals(
        McpModule.Transport.STATELESS_STREAMABLE_HTTP,
        McpModule.Transport.of("stateless-streamable-http"));
    assertEquals(McpModule.Transport.WEBSOCKET, McpModule.Transport.of("web-socket"));

    assertEquals("sse", McpModule.Transport.SSE.getValue());

    assertThrows(IllegalArgumentException.class, () -> McpModule.Transport.of("unknown"));
  }

  // --- CONFIG MISSING / ERROR TESTS ---

  @Test
  void testInstall_MissingConfig_ThrowsStartupException() {
    lenient().when(config.hasPath(anyString())).thenReturn(false);

    McpModule module = new McpModule(service2); // 'custom' serverKey

    assertThrows(StartupException.class, () -> installSafely(module));
  }

  @Test
  void testInstall_UnsupportedTransport_ThrowsIllegalStateException() {
    try (MockedConstruction<McpServerConfig> mocked =
        mockConstruction(
            McpServerConfig.class,
            (mock, context) -> {
              lenient().when(mock.getName()).thenReturn("test-server");
              lenient().when(mock.getVersion()).thenReturn("1.0.0");
              lenient().when(mock.getInstructions()).thenReturn("Test Instructions");

              // Force it past the stateless IF check, but crash the switch statement default block
              lenient()
                  .when(mock.getTransport())
                  .thenReturn(McpModule.Transport.SSE)
                  .thenReturn(McpModule.Transport.STATELESS_STREAMABLE_HTTP);
            })) {
      lenient().when(config.hasPath(anyString())).thenReturn(false);

      McpModule module = new McpModule(service1); // 'default' serverKey

      assertThrows(IllegalStateException.class, () -> installSafely(module));
    }
  }

  // --- STATELESS TRANSPORT TESTS ---

  @Test
  void testInstall_StatelessStreamableHttp() throws Exception {
    lenient().when(config.hasPath(anyString())).thenReturn(false);
    lenient().when(config.hasPath("mcp.generateOutputSchema")).thenReturn(true);
    lenient().when(config.getBoolean("mcp.generateOutputSchema")).thenReturn(true);
    lenient().when(config.hasPath("mcp.default.generateOutputSchema")).thenReturn(false);

    McpModule module =
        new McpModule(service1)
            .generateOutputSchema(false)
            .transport(McpModule.Transport.STATELESS_STREAMABLE_HTTP);

    // FIX: Using an empty list here achieves 100% coverage on the flatMap mapping
    // without crashing the SDK's ConcurrentHashMap due to mocked null names!
    lenient().when(service1.statelessCompletions(app)).thenReturn(List.of());

    // Mock capabilities
    doAnswer(
            inv -> {
              McpSchema.ServerCapabilities.Builder b = inv.getArgument(0);
              b.tools(true);
              b.prompts(true);
              b.resources(true, true);
              return null;
            })
        .when(service1)
        .capabilities(any());

    installSafely(module);

    // Verify Output Schema configuration propagation
    verify(service1).generateOutputSchema(true);

    // Execute app lifecycle hooks to hit logging & closing branches
    ArgumentCaptor<AutoCloseable> onStopCap = ArgumentCaptor.forClass(AutoCloseable.class);
    ArgumentCaptor<SneakyThrows.Runnable> onStartingCap =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);

    verify(app).onStop(onStopCap.capture());
    verify(app).onStarting(onStartingCap.capture());

    onStopCap.getValue().close();
    onStartingCap
        .getValue()
        .run(); // Prints stateless startup logs covering Tools, Prompts, Resources
  }

  // --- STATEFUL TRANSPORT TESTS ---

  @Test
  void testInstall_StreamableHttp_AndInvokerChaining() throws Exception {
    lenient().when(config.hasPath(anyString())).thenReturn(false);

    // Create a scenario where Otel tracing and multiple custom invokers are chained
    OtelMcpTracing otel1 = mock(OtelMcpTracing.class);
    OtelMcpTracing otel2 = mock(OtelMcpTracing.class); // Overrides otel1

    McpInvoker inv1 = mock(McpInvoker.class);
    McpInvoker inv2 = mock(McpInvoker.class);

    lenient().when(otel2.then(any())).thenReturn(otel2);
    lenient().when(inv2.then(any())).thenReturn(inv2);

    McpModule module =
        new McpModule(service1, service1) // Tests varargs constructor
            .invoker(otel1)
            .invoker(otel2)
            .invoker(inv1)
            .invoker(inv2); // inv2 then inv1 then otel2

    // FIX: Prevents NPEs during SDK's ConcurrentHashMap registration
    lenient().when(service1.completions(app)).thenReturn(List.of());

    installSafely(module);

    ArgumentCaptor<AutoCloseable> onStopCap = ArgumentCaptor.forClass(AutoCloseable.class);
    ArgumentCaptor<SneakyThrows.Runnable> onStartingCap =
        ArgumentCaptor.forClass(SneakyThrows.Runnable.class);

    verify(app).onStop(onStopCap.capture());
    verify(app).onStarting(onStartingCap.capture());

    onStopCap.getValue().close();
    onStartingCap.getValue().run(); // Prints stateful startup logs with NO tools/prompts
  }

  @Test
  void testInstall_SseTransport() {
    // Utilize Typesafe Config to mock a custom server definition
    Config mockConfig =
        ConfigFactory.parseString(
            "name: sse-server\n"
                + "version: 2.0\n"
                + "transport: sse\n"
                + "mcpEndpoint: /mcp/sse\n"
                + "messageEndpoint: /mcp/msg\n"
                + "sseEndpoint: /mcp/sse");

    // Lenient fallback for global config lookups
    lenient().when(config.hasPath(anyString())).thenReturn(false);
    lenient().when(config.hasPath("mcp.custom")).thenReturn(true);
    lenient().when(config.getConfig("mcp.custom")).thenReturn(mockConfig);

    McpModule module = new McpModule(service2); // 'custom'

    installSafely(module);

    verify(app).sse(eq("/mcp/sse"), any());
  }

  @Test
  void testInstall_WebSocketTransport() {
    Config mockConfig =
        ConfigFactory.parseString(
            "name: ws-server\n"
                + "version: 3.0\n"
                + "transport: web-socket\n"
                + "mcpEndpoint: /mcp/ws");

    // Lenient fallback for global config lookups
    lenient().when(config.hasPath(anyString())).thenReturn(false);
    lenient().when(config.hasPath("mcp.custom")).thenReturn(true);
    lenient().when(config.getConfig("mcp.custom")).thenReturn(mockConfig);

    McpModule module = new McpModule(service2);

    installSafely(module);

    verify(app).ws(eq("/mcp/ws"), any());
  }

  // --- INTERNAL HELPER TESTS ---

  @Test
  void testCtxExtractor() throws Exception {
    Field field = McpModule.class.getDeclaredField("CTX_EXTRACTOR");
    field.setAccessible(true);
    McpTransportContextExtractor<Context> extractor =
        (McpTransportContextExtractor<Context>) field.get(null);

    Context mockCtx = mock(Context.class);
    Map<String, String> mockHeaders = Map.of("Auth", "Token");
    lenient().when(mockCtx.headerMap()).thenReturn(mockHeaders);

    McpTransportContext mcpTransportContext = extractor.extract(mockCtx);

    assertNotNull(mcpTransportContext);
  }
}
