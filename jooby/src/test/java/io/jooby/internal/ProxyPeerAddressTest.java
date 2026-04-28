/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.Context;
import io.jooby.value.Value;

public class ProxyPeerAddressTest {

  private void mockHeader(Context ctx, String name, String value) {
    Value valMock = mock(Value.class);
    if (value == null) {
      when(valMock.toOptional()).thenReturn(Optional.empty());
      when(valMock.valueOrNull()).thenReturn(null);
    } else {
      when(valMock.toOptional()).thenReturn(Optional.of(value));
      when(valMock.valueOrNull()).thenReturn(value);
    }
    when(ctx.header(name)).thenReturn(valMock);
  }

  private Context setupBaseContext() {
    Context ctx = mock(Context.class);
    when(ctx.getRemoteAddress()).thenReturn("127.0.0.1");
    when(ctx.getScheme()).thenReturn("http");
    when(ctx.getHost()).thenReturn("localhost");
    when(ctx.getServerPort()).thenReturn(8080);
    when(ctx.isSecure()).thenReturn(false);

    mockHeader(ctx, "X-Forwarded-For", null);
    mockHeader(ctx, "X-Forwarded-Proto", null);
    mockHeader(ctx, "X-Forwarded-Host", null);
    mockHeader(ctx, "X-Forwarded-Port", null);

    return ctx;
  }

  @Test
  @DisplayName("Test default context fallbacks (no headers present)")
  void testParseNoHeaders() {
    Context ctx = setupBaseContext();
    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    assertEquals("127.0.0.1", peer.getRemoteAddress());
    assertEquals("http", peer.getScheme());
    assertEquals("localhost", peer.getHost());
    assertEquals(8080, peer.getPort()); // Hits defaultPort() -> localhost branch
  }

  @Test
  @DisplayName("Test comma-separated values and space trimming")
  void testParseCommaSeparatedHeaders() {
    Context ctx = setupBaseContext();
    // Tests the added trim() logic for both branch paths in mostRecent()
    mockHeader(ctx, "X-Forwarded-For", " 192.168.1.1 , 10.0.0.1 ");
    mockHeader(ctx, "X-Forwarded-Proto", "https, http");
    mockHeader(ctx, "X-Forwarded-Host", " jooby.io , other.io ");
    mockHeader(ctx, "X-Forwarded-Port", " 8443 , 80 ");

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    assertEquals("192.168.1.1", peer.getRemoteAddress());
    assertEquals("https", peer.getScheme());
    assertEquals("jooby.io", peer.getHost());
    assertEquals(8443, peer.getPort());
  }

  @Test
  @DisplayName("Test IPv4/Hostname with trailing port in host")
  void testParseIPv4WithPort() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-Host", "api.jooby.io:9090");

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    assertEquals("api.jooby.io", peer.getHost());
    assertEquals(9090, peer.getPort());
  }

  @Test
  @DisplayName("Test X-Forwarded-Port taking precedence over Host port")
  void testPortPrecedence() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-Host", "api.jooby.io:9090");
    mockHeader(ctx, "X-Forwarded-Port", "3000"); // Explicit port should win

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    assertEquals("api.jooby.io", peer.getHost());
    assertEquals(3000, peer.getPort());
  }

  @Test
  @DisplayName("Test IPv6 Address parsing without port")
  void testParseIPv6NoPort() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-Host", "[2001:db8::1]");
    when(ctx.isSecure()).thenReturn(true);

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    // Hits the `index == -1` branch after finding `]`
    assertEquals("[2001:db8::1]", peer.getHost());
    assertEquals(443, peer.getPort()); // Hits defaultPort() -> secure branch
  }

  @Test
  @DisplayName("Test IPv6 Address parsing with port")
  void testParseIPv6WithPort() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-Host", "[2001:db8::1]:8081");

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    assertEquals("[2001:db8::1]", peer.getHost());
    assertEquals(8081, peer.getPort());
  }

  @Test
  @DisplayName("Test Malformed IPv6 Address parsing (missing closing bracket)")
  void testParseMalformedIPv6() {
    Context ctx = setupBaseContext();
    // Tests the fix where end == -1 bypasses the colon port search
    mockHeader(ctx, "X-Forwarded-Host", "[2001:db8:1");

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    // Host remains fully intact, port drops to default
    assertEquals("[2001:db8:1", peer.getHost());
    assertEquals(80, peer.getPort());
  }

  @Test
  @DisplayName("Test Port exception handling (NumberFormatException fallback)")
  void testParseInvalidPortHeader() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-Host", "remote.io");
    mockHeader(ctx, "X-Forwarded-Port", "not-a-number");
    when(ctx.isSecure()).thenReturn(false);

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);

    // Hits the `catch (NumberFormatException ignore)` branch
    assertEquals("remote.io", peer.getHost());
    assertEquals(80, peer.getPort()); // Hits defaultPort() -> insecure branch
  }

  @Test
  @DisplayName("Test apply modifications to Context via set()")
  void testSetMethod() {
    Context ctx = setupBaseContext();
    mockHeader(ctx, "X-Forwarded-For", "10.0.0.1");
    mockHeader(ctx, "X-Forwarded-Proto", "wss");
    mockHeader(ctx, "X-Forwarded-Host", "socket.io:3000");

    ProxyPeerAddress peer = ProxyPeerAddress.parse(ctx);
    peer.set(ctx);

    verify(ctx).setRemoteAddress("10.0.0.1");
    verify(ctx).setScheme("wss");
    verify(ctx).setHost("socket.io");
    verify(ctx).setPort(3000);
  }
}
