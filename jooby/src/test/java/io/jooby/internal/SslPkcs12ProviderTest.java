/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.security.KeyStore;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import io.jooby.SslOptions;

public class SslPkcs12ProviderTest {

  private SslPkcs12Provider provider;

  @BeforeEach
  void setUp() {
    provider = new SslPkcs12Provider();
  }

  /**
   * Helper method to generate a valid, empty PKCS12 keystore in memory. This avoids exceptions
   * during KeyStore.load() and kmf.init().
   */
  private byte[] createEmptyKeystore(String password) throws Exception {
    KeyStore ks = KeyStore.getInstance("PKCS12");
    ks.load(null, null); // Initialize empty
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ks.store(out, password == null ? new char[0] : password.toCharArray());
    return out.toByteArray();
  }

  @Test
  @DisplayName("Test supports() branch logic for PKCS12")
  void testSupports() {
    assertTrue(provider.supports("PKCS12"));
    assertTrue(provider.supports("pkcs12")); // Ignore case
    assertFalse(provider.supports("JKS"));
    assertFalse(provider.supports(null));
  }

  @Test
  @DisplayName("Test SSLContext creation without provider and without trust certificates")
  void testCreateNoProviderNoTrustCert() throws Exception {
    byte[] ksData = createEmptyKeystore("password");

    SslOptions options = mock(SslOptions.class);
    when(options.getType()).thenReturn("PKCS12");
    when(options.getPassword()).thenReturn("password");
    when(options.getCert()).thenReturn(new ByteArrayInputStream(ksData));

    // Trigger the options.getTrustCert() == null branch
    when(options.getTrustCert()).thenReturn(null);

    // Trigger the provider == null branch
    SSLContext ctx = provider.create(getClass().getClassLoader(), null, options);

    assertNotNull(ctx);
    assertEquals("TLS", ctx.getProtocol());

    // Verify try-with-resources properly closed the SslOptions
    verify(options).close();
  }

  @Test
  @DisplayName("Test SSLContext creation with explicit provider and with trust certificates")
  void testCreateWithProviderAndTrustCert() throws Exception {
    byte[] ksData = createEmptyKeystore("password");

    SslOptions options = mock(SslOptions.class);
    when(options.getType()).thenReturn("PKCS12");
    when(options.getPassword()).thenReturn("password");
    when(options.getCert()).thenReturn(new ByteArrayInputStream(ksData));

    // Trigger the options.getTrustCert() != null branch
    when(options.getTrustCert()).thenReturn(new ByteArrayInputStream(ksData));
    when(options.getTrustPassword()).thenReturn("password");

    // Trigger the provider != null branch (using a standard built-in provider like SunJSSE)
    SSLContext ctx = provider.create(getClass().getClassLoader(), "SunJSSE", options);

    assertNotNull(ctx);
    assertEquals("SunJSSE", ctx.getProvider().getName());
  }

  @Test
  @DisplayName("Test exception propagation and null password ternary branch")
  void testNullPasswordAndExceptionPropagation() {
    SslOptions options = mock(SslOptions.class);
    when(options.getType()).thenReturn("PKCS12");

    // Return null to hit the `password == null ? null : password.toCharArray()` branch
    when(options.getPassword()).thenReturn(null);

    // Return a garbage byte array to force an exception inside KeyStore.load()
    when(options.getCert()).thenReturn(new ByteArrayInputStream(new byte[] {1, 2, 3}));

    // Verify the Exception catch block propagates via SneakyThrows
    assertThrows(
        EOFException.class,
        () -> {
          provider.create(getClass().getClassLoader(), null, options);
        });
  }
}
