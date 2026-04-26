/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.validation;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.Route;
import io.jooby.exception.RegistryException;

public class BeanValidatorTest {

  private Context ctx;
  private BeanValidator validator;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    validator = mock(BeanValidator.class);
  }

  @Test
  void applyNullBean() {
    Object result = BeanValidator.apply(ctx, null);
    assertNull(result);
    verifyNoInteractions(ctx);
  }

  @Test
  void applyMissingValidatorDependency() {
    when(ctx.require(BeanValidator.class)).thenThrow(new RegistryException("not found"));
    RegistryException ex =
        assertThrows(RegistryException.class, () -> BeanValidator.apply(ctx, new Object()));
    assertTrue(ex.getMessage().contains("Unable to load 'BeanValidator' class"));
  }

  @Test
  void applySingleObject() {
    when(ctx.require(BeanValidator.class)).thenReturn(validator);
    Object bean = new Object();

    BeanValidator.apply(ctx, bean);

    verify(validator).validate(ctx, bean);
  }

  @Test
  void applyIterable() {
    when(ctx.require(BeanValidator.class)).thenReturn(validator);
    List<String> list = List.of("a", "b");

    BeanValidator.apply(ctx, list);

    verify(validator).validate(ctx, "a");
    verify(validator).validate(ctx, "b");
  }

  @Test
  void applyArray() {
    when(ctx.require(BeanValidator.class)).thenReturn(validator);
    String[] array = {"a", "b"};

    BeanValidator.apply(ctx, array);

    verify(validator).validate(ctx, "a");
    verify(validator).validate(ctx, "b");
  }

  @Test
  void applyMap() {
    when(ctx.require(BeanValidator.class)).thenReturn(validator);
    Map<String, String> map = Map.of("key", "value");

    BeanValidator.apply(ctx, map);

    verify(validator).validate(ctx, "value");
  }

  @Test
  void validateAsFilter() {
    Route.Filter filter = BeanValidator.validate();
    assertNotNull(filter);
    // Since it's a method reference to BeanValidator::validate, this satisfies coverage
  }

  @Test
  void validateAsHandlerWithAttributePresent() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    Route route = mock(Route.class);
    when(ctx.getRoute()).thenReturn(route);
    when(route.getAttributes()).thenReturn(Map.of(BeanValidator.class.getName(), "present"));

    Route.Handler handler = BeanValidator.validate(next);
    handler.apply(ctx);

    // Should NOT wrap in ValidationContext
    verify(next).apply(ctx);
  }

  @Test
  void validateAsHandlerWrapValidationContext() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    Route route = mock(Route.class);
    when(ctx.getRoute()).thenReturn(route);
    when(route.getAttributes()).thenReturn(Map.of());

    Route.Handler handler = BeanValidator.validate(next);
    handler.apply(ctx);

    // Should wrap in ValidationContext
    verify(next).apply(any(ValidationContext.class));
  }

  @Test
  void getRootCauseWithReflectionExceptions() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    Route route = mock(Route.class);
    when(ctx.getRoute()).thenReturn(route);
    when(route.getAttributes()).thenReturn(Map.of());

    RuntimeException root = new RuntimeException("root");
    InvocationTargetException ite = new InvocationTargetException(root);

    when(next.apply(any())).thenThrow(ite);

    Route.Handler handler = BeanValidator.validate(next);
    RuntimeException thrown = assertThrows(RuntimeException.class, () -> handler.apply(ctx));
    assertEquals("root", thrown.getMessage());
  }

  @Test
  void getRootCauseDeepChain() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    Route route = mock(Route.class);
    when(ctx.getRoute()).thenReturn(route);
    when(route.getAttributes()).thenReturn(Map.of());

    // Deep chain to trigger the "advanceSlowPointer" logic in getRootCause
    Exception e1 = new Exception("1");
    Exception e2 = new Exception("2", e1);
    Exception e3 = new Exception("3", e2);
    Exception e4 = new Exception("4", e3);
    UndeclaredThrowableException ute = new UndeclaredThrowableException(e4);

    when(next.apply(any())).thenThrow(ute);

    Route.Handler handler = BeanValidator.validate(next);
    var thrown = assertThrows(Exception.class, () -> handler.apply(ctx));
    assertEquals("1", thrown.getMessage());
  }

  @Test
  void getRootCauseLoopDetection() throws Exception {
    Route.Handler next = mock(Route.Handler.class);
    Route route = mock(Route.class);
    when(ctx.getRoute()).thenReturn(route);
    when(route.getAttributes()).thenReturn(Map.of());

    // Circular cause
    class CircularException extends Exception {
      CircularException(String m) {
        super(m);
      }

      void setC(Throwable t) {
        initCause(t);
      }
    }
    CircularException ex1 = new CircularException("ex1");
    CircularException ex2 = new CircularException("ex2");
    ex1.initCause(ex2);
    ex2.initCause(ex1);

    UndeclaredThrowableException ute = new UndeclaredThrowableException(ex1);
    when(next.apply(any())).thenThrow(ute);

    Route.Handler handler = BeanValidator.validate(next);
    IllegalArgumentException loop =
        assertThrows(IllegalArgumentException.class, () -> handler.apply(ctx));
    assertEquals("Loop in causal chain detected.", loop.getMessage());
  }
}
