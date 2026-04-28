/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import io.jooby.Context;

public class ContextInitializerTest {

  @Test
  @DisplayName("Verify PROXY_PEER_ADDRESS executes ProxyPeerAddress parsing and setting")
  void testProxyPeerAddressInitializer() {
    Context ctx = mock(Context.class);
    ProxyPeerAddress mockAddress = mock(ProxyPeerAddress.class);

    try (MockedStatic<ProxyPeerAddress> mockedStatic = mockStatic(ProxyPeerAddress.class)) {
      mockedStatic.when(() -> ProxyPeerAddress.parse(ctx)).thenReturn(mockAddress);

      // Execute the static initializer
      ContextInitializer.PROXY_PEER_ADDRESS.apply(ctx);

      // Verify it parsed and then set the address on the context
      mockedStatic.verify(() -> ProxyPeerAddress.parse(ctx));
      verify(mockAddress).set(ctx);
    }
  }

  @Test
  @DisplayName("Verify the default add method returns the provided initializer")
  void testDefaultAddMethod() {
    ContextInitializer base =
        ctx -> {
          /* no-op */
        };
    ContextInitializer next =
        ctx -> {
          /* no-op */
        };

    // The current implementation of add() simply returns the argument
    ContextInitializer result = base.add(next);

    assertSame(next, result, "The default add method should return the passed initializer.");
  }
}
