/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.undertow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import java.lang.reflect.Constructor;

import javax.net.ssl.SSLEngine;

import org.conscrypt.Conscrypt;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

class ConscriptAlpnProviderTest {

  @Test
  void testIsEnabled_False() {
    // A standard Mockito mock will have a class name like "javax.net.ssl.SSLEngine$MockitoMock$..."
    SSLEngine engine = mock(SSLEngine.class);
    ConscriptAlpnProvider provider = new ConscriptAlpnProvider();

    assertFalse(provider.isEnabled(engine));
  }

  @Test
  void testIsEnabled_True() {
    // Dynamically generate a class in the "org.conscrypt" package to satisfy the startsWith check
    Class<? extends SSLEngine> fakeEngineClass =
        new ByteBuddy()
            .subclass(SSLEngine.class)
            .name("org.conscrypt.FakeSSLEngine")
            .make()
            .load(
                Thread.currentThread().getContextClassLoader(),
                ClassLoadingStrategy.Default.WRAPPER)
            .getLoaded();

    SSLEngine engine = mock(fakeEngineClass);
    ConscriptAlpnProvider provider = new ConscriptAlpnProvider();

    assertTrue(provider.isEnabled(engine));
  }

  @Test
  void testSetProtocols() {
    SSLEngine engine = mock(SSLEngine.class);
    String[] protocols = {"h2", "http/1.1"};

    try (MockedStatic<Conscrypt> conscrypt = mockStatic(Conscrypt.class)) {
      ConscriptAlpnProvider provider = new ConscriptAlpnProvider();
      SSLEngine result = provider.setProtocols(engine, protocols);

      assertSame(engine, result);
      conscrypt.verify(() -> Conscrypt.setApplicationProtocols(engine, protocols));
    }
  }

  @Test
  void testGetSelectedProtocol() {
    SSLEngine engine = mock(SSLEngine.class);

    try (MockedStatic<Conscrypt> conscrypt = mockStatic(Conscrypt.class)) {
      conscrypt.when(() -> Conscrypt.getApplicationProtocol(engine)).thenReturn("h2");

      ConscriptAlpnProvider provider = new ConscriptAlpnProvider();
      String result = provider.getSelectedProtocol(engine);

      assertEquals("h2", result);
    }
  }

  @Test
  void testGetPriority() {
    ConscriptAlpnProvider provider = new ConscriptAlpnProvider();
    assertEquals(400, provider.getPriority());
  }

  @Test
  void testImplPrivateConstructorCoverage() throws Exception {
    // Achieves 100% line coverage for the implicitly generated private constructor
    // of the nested static "Impl" class that JaCoCo otherwise flags.
    Constructor<?> constructor =
        Class.forName("io.jooby.internal.undertow.ConscriptAlpnProvider$Impl")
            .getDeclaredConstructor();
    constructor.setAccessible(true);
    constructor.newInstance();
  }
}
