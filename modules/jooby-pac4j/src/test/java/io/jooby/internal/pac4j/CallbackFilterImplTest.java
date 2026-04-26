/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.pac4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.pac4j.core.engine.CallbackLogic;

import io.jooby.Context;
import io.jooby.Router;
import io.jooby.ServiceRegistry;
import io.jooby.pac4j.Pac4jOptions;

public class CallbackFilterImplTest {

  private Pac4jOptions options;
  private CallbackLogic callbackLogic;
  private Context ctx;
  private CallbackFilterImpl callbackFilter;

  @BeforeEach
  void setUp() {
    options = new Pac4jOptions();
    callbackLogic = mock(CallbackLogic.class);
    options.setCallbackLogic(callbackLogic);

    ctx = mock(Context.class);

    // Mock Router/Registry chain required by Pac4jFrameworkParameters.create(ctx)
    Router router = mock(Router.class);
    when(ctx.getRouter()).thenReturn(router);
    when(router.getServices()).thenReturn(mock(ServiceRegistry.class));

    callbackFilter = new CallbackFilterImpl(options);
  }

  @Test
  void testCallbackWithResult() throws Exception {
    Object result = "HandledByPac4j";
    when(callbackLogic.perform(any(), any(), any(), any(), any())).thenReturn(result);

    Object actual = callbackFilter.apply(ctx);

    assertEquals(result, actual);
    verify(callbackLogic)
        .perform(
            eq(options),
            eq(options.getDefaultUrl()),
            eq(options.getRenewSession()),
            eq(options.getDefaultClient()),
            any());
  }

  @Test
  void testCallbackWithNullResultReturnsContext() throws Exception {
    // Pac4j sometimes returns null if it doesn't produce a response directly
    when(callbackLogic.perform(any(), any(), any(), any(), any())).thenReturn(null);

    Object actual = callbackFilter.apply(ctx);

    // Should return the Jooby context as per logic: result == null ? ctx : result
    assertEquals(ctx, actual);
  }

  @Test
  void testExceptionPropagationWithCause() {
    Exception cause = new Exception("Pac4j failed");
    RuntimeException wrapper = new RuntimeException(cause);

    when(callbackLogic.perform(any(), any(), any(), any(), any())).thenThrow(wrapper);

    Exception ex = assertThrows(Exception.class, () -> callbackFilter.apply(ctx));
    assertEquals("Pac4j failed", ex.getMessage());
  }

  @Test
  void testExceptionPropagationWithoutCause() {
    RuntimeException simple = new RuntimeException("Simple error");

    when(callbackLogic.perform(any(), any(), any(), any(), any())).thenThrow(simple);

    RuntimeException ex = assertThrows(RuntimeException.class, () -> callbackFilter.apply(ctx));
    assertEquals("Simple error", ex.getMessage());
  }
}
