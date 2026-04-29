/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.handler;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.ServerOptions;

public class SSLHandlerTest {

  private Context ctx;

  @BeforeEach
  void setUp() {
    ctx = mock(Context.class);
    when(ctx.getRequestPath()).thenReturn("/api/data");
    when(ctx.queryString()).thenReturn("?foo=bar");
  }

  @Test
  @DisplayName("Verify secure requests exit immediately without redirecting")
  void testAlreadySecure() {
    when(ctx.isSecure()).thenReturn(true);

    SSLHandler handler = new SSLHandler();
    handler.apply(ctx);

    verify(ctx).isSecure();
    verifyNoMoreInteractions(ctx);
  }

  @Test
  @DisplayName("Verify redirect with explicit host and explicit custom port")
  void testExplicitHostAndPort() {
    when(ctx.isSecure()).thenReturn(false);

    SSLHandler handler = new SSLHandler("custom-domain.com", 8443);
    handler.apply(ctx);

    verify(ctx).sendRedirect("https://custom-domain.com:8443/api/data?foo=bar");
  }

  @Test
  @DisplayName("Verify redirect with explicit host and default secure port (443)")
  void testExplicitHostDefaultPort() {
    when(ctx.isSecure()).thenReturn(false);

    SSLHandler handler = new SSLHandler("custom-domain.com");
    handler.apply(ctx);

    // Port 443 is not appended to the URL
    verify(ctx).sendRedirect("https://custom-domain.com/api/data?foo=bar");
  }

  @Test
  @DisplayName("Verify redirect extracts host from Context when host has a port")
  void testDynamicHostWithPort() {
    when(ctx.isSecure()).thenReturn(false);
    // Context provides host with a port attached
    when(ctx.getHostAndPort()).thenReturn("dynamic-domain.com:8080");

    SSLHandler handler = new SSLHandler(8443); // Explicit port
    handler.apply(ctx);

    // Should strip the 8080 and append 8443
    verify(ctx).sendRedirect("https://dynamic-domain.com:8443/api/data?foo=bar");
  }

  @Test
  @DisplayName("Verify redirect extracts host from Context when host has no port")
  void testDynamicHostWithoutPort() {
    when(ctx.isSecure()).thenReturn(false);
    // Context provides host without a port
    when(ctx.getHostAndPort()).thenReturn("dynamic-domain.com");

    SSLHandler handler = new SSLHandler(); // Default port 443
    handler.apply(ctx);

    verify(ctx).sendRedirect("https://dynamic-domain.com/api/data?foo=bar");
  }

  @Test
  @DisplayName("Verify localhost special logic successfully retrieves port from ServerOptions")
  void testLocalhostWithServerOptionsPort() {
    when(ctx.isSecure()).thenReturn(false);
    when(ctx.getHostAndPort()).thenReturn("localhost:8080");

    ServerOptions serverOptions = mock(ServerOptions.class);
    when(serverOptions.getSecurePort()).thenReturn(8443);
    when(ctx.require(ServerOptions.class)).thenReturn(serverOptions);

    SSLHandler handler = new SSLHandler();
    handler.apply(ctx);

    verify(ctx).sendRedirect("https://localhost:8443/api/data?foo=bar");
  }

  @Test
  @DisplayName(
      "Verify localhost special logic falls back cleanly when ServerOptions secure port is null")
  void testLocalhostWithNullServerOptionsPort() {
    when(ctx.isSecure()).thenReturn(false);
    when(ctx.getHostAndPort()).thenReturn("localhost:8080");

    ServerOptions serverOptions = mock(ServerOptions.class);
    when(serverOptions.getSecurePort()).thenReturn(null);
    when(ctx.require(ServerOptions.class)).thenReturn(serverOptions);

    SSLHandler handler = new SSLHandler();
    handler.apply(ctx);

    verify(ctx).sendRedirect("https://localhost/api/data?foo=bar");
  }

  @Test
  @DisplayName("Verify negative port bypasses port appending")
  void testNegativePortBypass() {
    when(ctx.isSecure()).thenReturn(false);
    when(ctx.getHostAndPort()).thenReturn("dynamic-domain.com");

    // Setting a negative port should skip the append logic
    SSLHandler handler = new SSLHandler(-1);
    handler.apply(ctx);

    verify(ctx).sendRedirect("https://dynamic-domain.com/api/data?foo=bar");
  }
}
