/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.jsonrpc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import io.jooby.Context;
import io.jooby.Jooby;
import io.jooby.annotation.Generated;
import io.jooby.internal.jsonrpc.JsonRpcHandler;
import io.jooby.jsonrpc.instrumentation.OtelJsonRcpTracing;

@ExtendWith(MockitoExtension.class)
class JsonRpcModuleTest {

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  private Jooby app;

  public static class DummyDispatcher {}

  @Generated(DummyDispatcher.class)
  private record DummyService(List<String> methods) implements JsonRpcService {

    @Override
    public List<String> getMethods() {
      return methods;
    }

    @Override
    public void install(Jooby application) throws Exception {}

    @Override
    public Object execute(Context ctx, JsonRpcRequest req) throws Exception {
      return null;
    }
  }

  private JsonRpcService service1;
  private JsonRpcService service2;

  @BeforeEach
  void setUp() {
    service1 = new DummyService(List.of("method1", "method2"));
    service2 = new DummyService(List.of("method3"));
  }

  @Test
  void testDefaultConstructorAndRegistryMapping() throws Exception {
    JsonRpcModule module = new JsonRpcModule(service1, service2);
    module.install(app);

    ArgumentCaptor<JsonRpcHandler> handlerCaptor = ArgumentCaptor.forClass(JsonRpcHandler.class);
    verify(app).post(eq("/rpc"), handlerCaptor.capture());
    assertNotNull(handlerCaptor.getValue());
  }

  @Test
  void testCustomPathConstructor() throws Exception {
    JsonRpcModule module = new JsonRpcModule("/api/rpc", service1);
    module.install(app);

    verify(app).post(eq("/api/rpc"), any(JsonRpcHandler.class));
  }

  @Test
  void testInvokerChainingGeneratesAThenBPipeline() throws Exception {
    JsonRpcInvoker invokerA = mock(JsonRpcInvoker.class);
    JsonRpcInvoker invokerB = mock(JsonRpcInvoker.class);
    JsonRpcInvoker chained = mock(JsonRpcInvoker.class);

    // Mock the chaining behavior to ensure A wraps B
    when(invokerA.then(invokerB)).thenReturn(chained);

    JsonRpcModule module = new JsonRpcModule("/rpc", service1);

    // Applying .invoker(A).invoker(B)
    module.invoker(invokerA).invoker(invokerB);

    module.install(app);

    // Asserts that A.then(B) was the exact method invoked internally
    verify(invokerA).then(invokerB);
  }

  @Test
  void testOtelTracingIsAlwaysPromotedToHeadRegardlessOfOrder() throws Exception {
    JsonRpcInvoker regularInvoker = mock(JsonRpcInvoker.class);
    OtelJsonRcpTracing otelTracer = mock(OtelJsonRcpTracing.class);
    JsonRpcInvoker chained = mock(JsonRpcInvoker.class);

    // Otel bypasses the A -> B pipeline and always becomes the root
    when(otelTracer.then(regularInvoker)).thenReturn(chained);

    JsonRpcModule module = new JsonRpcModule("/rpc", service1);

    // We add regular first, then OTEL.
    module.invoker(regularInvoker).invoker(otelTracer);

    module.install(app);

    // Asserts that OTEL still wrapped the regular invoker
    verify(otelTracer).then(regularInvoker);
  }

  @Test
  void testOtelTracingWithNoOtherInvokers() throws Exception {
    OtelJsonRcpTracing otelTracer = mock(OtelJsonRcpTracing.class);

    JsonRpcModule module = new JsonRpcModule("/rpc", service1);
    module.invoker(otelTracer);

    module.install(app);

    ArgumentCaptor<JsonRpcHandler> handlerCaptor = ArgumentCaptor.forClass(JsonRpcHandler.class);
    verify(app).post(eq("/rpc"), handlerCaptor.capture());

    assertNotNull(handlerCaptor.getValue());
  }
}
