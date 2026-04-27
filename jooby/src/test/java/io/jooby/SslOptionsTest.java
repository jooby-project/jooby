/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class SslOptionsTest {

  @Test
  public void shouldDoNothingOnMissingPaths() {
    Config config = ConfigFactory.empty().resolve();

    assertEquals(false, SslOptions.from(config).isPresent());
  }

  @Test
  public void shouldFailOnInvalidSslType() {
    Config config = ConfigFactory.empty().withValue("ssl.type", fromAnyRef("xxxx")).resolve();

    assertThrows(UnsupportedOperationException.class, () -> SslOptions.from(config));
  }

  @Test
  public void shouldLoadSelfSigned() {
    Config config =
        ConfigFactory.empty().withValue("ssl.type", fromAnyRef("self-signed")).resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.PKCS12, options.getType());
    assertNotNull(options.getCert());
    assertEquals("changeit", options.getPassword());
  }

  @Test
  public void shouldLoadPKCS12FromConfig() {
    Config config =
        ConfigFactory.empty()
            .withValue("ssl.type", fromAnyRef("pkcs12"))
            .withValue("ssl.cert", fromAnyRef("ssl/test.p12"))
            .withValue("ssl.password", fromAnyRef("changeit"))
            .withValue("ssl.trust.cert", fromAnyRef("ssl/trust.p12"))
            .withValue("ssl.trust.password", fromAnyRef("pass"))
            .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.PKCS12, options.getType());
    assertNotNull(options.getCert());
    assertEquals("changeit", options.getPassword());
    assertNotNull(options.getTrustCert());
    assertEquals("pass", options.getTrustPassword());
  }

  @Test
  public void shouldLoadX509FromConfig() {
    Config config =
        ConfigFactory.empty()
            .withValue("ssl.type", fromAnyRef("x509"))
            .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
            .withValue("ssl.key", fromAnyRef("ssl/test.key"))
            .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.X509, options.getType());
    assertNotNull(options.getCert());
    assertNotNull(options.getPrivateKey());
  }

  @Test
  public void shouldLoadX509WithPasswordFromConfig() {
    Config config =
        ConfigFactory.empty()
            .withValue("ssl.type", fromAnyRef("x509"))
            .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
            .withValue("ssl.key", fromAnyRef("ssl/test.key"))
            .withValue("ssl.password", fromAnyRef("changeit"))
            .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(SslOptions.X509, options.getType());
    assertNotNull(options.getCert());
    assertNotNull(options.getPrivateKey());
    assertEquals("changeit", options.getPassword());
  }

  @Test
  public void shouldParseSingleProtocol() {
    Config config =
        ConfigFactory.empty()
            .withValue("ssl.protocol", fromAnyRef("TLSv1.2"))
            .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
            .withValue("ssl.key", fromAnyRef("ssl/test.key"))
            .withValue("ssl.password", fromAnyRef("changeit"))
            .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(Collections.singletonList("TLSv1.2"), options.getProtocol());
  }

  @Test
  public void shouldParseProtocols() {
    Config config =
        ConfigFactory.empty()
            .withValue("ssl.protocol", fromAnyRef(Arrays.asList("TLSv1.2", "TLSv1.3")))
            .withValue("ssl.cert", fromAnyRef("ssl/test.crt"))
            .withValue("ssl.key", fromAnyRef("ssl/test.key"))
            .withValue("ssl.password", fromAnyRef("changeit"))
            .resolve();

    SslOptions options = SslOptions.from(config).get();
    assertEquals(Arrays.asList("TLSv1.2", "TLSv1.3"), options.getProtocol());
  }

  // --- NEW TESTS FOR 100% COVERAGE ---

  @Test
  public void accessorsAndProperties() {
    SslOptions opt = new SslOptions();
    assertEquals(SslOptions.PKCS12, opt.getType());

    opt.setType(SslOptions.X509);
    assertEquals(SslOptions.X509, opt.getType());

    opt.setPassword("pwd");
    assertEquals("pwd", opt.getPassword());

    opt.setTrustPassword("tpwd");
    assertEquals("tpwd", opt.getTrustPassword());

    InputStream dummyStream = mock(InputStream.class);
    opt.setCert(dummyStream);
    assertEquals(dummyStream, opt.getCert());

    opt.setTrustCert(dummyStream);
    assertEquals(dummyStream, opt.getTrustCert());

    opt.setPrivateKey(dummyStream);
    assertEquals(dummyStream, opt.getPrivateKey());
  }

  @Test
  public void shouldCloseInputStreamsSafely() throws Exception {
    InputStream cert = mock(InputStream.class);
    InputStream trust = mock(InputStream.class);
    InputStream key = mock(InputStream.class);

    // Force one to throw an IOException to cover the ignored exception block
    doThrow(new IOException("Ignored close error")).when(cert).close();

    SslOptions opts = new SslOptions().setCert(cert).setTrustCert(trust).setPrivateKey(key);

    assertDoesNotThrow(opts::close);

    verify(cert).close();
    verify(trust).close();
    verify(key).close();

    assertNull(opts.getCert());
    assertNull(opts.getTrustCert());
    assertNull(opts.getPrivateKey());
  }

  @Test
  public void getResourceAbsolute(@TempDir Path tempDir) throws Exception {
    Path file = tempDir.resolve("mycert.crt");
    Files.write(file, "certdata".getBytes());

    InputStream is = SslOptions.getResource(file.toAbsolutePath().toString());
    assertNotNull(is);
    is.close();
  }

  @Test
  public void getResourceRelative(@TempDir Path tempDir) throws Exception {
    String originalDir = System.getProperty("user.dir");
    try {
      System.setProperty("user.dir", tempDir.toAbsolutePath().toString());
      Path file = tempDir.resolve("mycert.crt");
      Files.write(file, "certdata".getBytes());

      InputStream is = SslOptions.getResource("mycert.crt");
      assertNotNull(is);
      is.close();
    } finally {
      System.setProperty("user.dir", originalDir);
    }
  }

  @Test
  public void getResourceInvalidPathException() {
    String originalDir = System.getProperty("user.dir");
    try {
      // Setting an invalid path for user.dir triggers InvalidPathException inside getResource
      System.setProperty("user.dir", "\u0000");

      FileNotFoundException ex =
          assertThrows(FileNotFoundException.class, () -> SslOptions.getResource("missing.txt"));
    } finally {
      System.setProperty("user.dir", originalDir);
    }
  }

  @Test
  public void getResourceIOExceptionOnDirectory(@org.junit.jupiter.api.io.TempDir Path tempDir)
      throws Exception {
    Path file = tempDir.resolve("mycert.crt");
    Files.write(file, "certdata".getBytes());

    try (org.mockito.MockedStatic<java.nio.file.Files> filesMock =
        org.mockito.Mockito.mockStatic(
            java.nio.file.Files.class, org.mockito.Mockito.CALLS_REAL_METHODS)) {
      // Intentionally force an IOException when trying to read this specific file
      filesMock
          .when(() -> java.nio.file.Files.newInputStream(file.toAbsolutePath()))
          .thenThrow(new java.io.IOException("Forced IO Error"));

      // SneakyThrows might propagate the raw IOException or wrap it in a RuntimeException.
      // Catching Exception.class ensures we safely catch both implementations.
      Exception ex =
          assertThrows(
              Exception.class, () -> SslOptions.getResource(file.toAbsolutePath().toString()));

      boolean isIoError =
          (ex instanceof java.io.IOException) || (ex.getCause() instanceof java.io.IOException);
      assertTrue(isIoError, "Expected an IOException or a wrapper containing it");
    }
  }

  @Test
  public void getResourceClasspathLeadingSlash() throws Exception {
    InputStream is = SslOptions.getResource("/ssl/test.crt");
    assertNotNull(is);
    is.close();
  }

  @Test
  public void staticFactories() {
    SslOptions pkcs12 = SslOptions.pkcs12("ssl/test.p12", "pass");
    assertEquals(SslOptions.PKCS12, pkcs12.getType());
    assertNotNull(pkcs12.getCert());
    assertEquals("pass", pkcs12.getPassword());

    SslOptions x509_1 = SslOptions.x509("ssl/test.crt", "ssl/test.key");
    assertEquals(SslOptions.X509, x509_1.getType());
    assertNotNull(x509_1.getCert());
    assertNotNull(x509_1.getPrivateKey());

    SslOptions x509_2 = SslOptions.x509("ssl/test.crt", "ssl/test.key", "pass");
    assertEquals("pass", x509_2.getPassword());
  }

  @Test
  public void selfSignedTypes() {
    SslOptions x509 = SslOptions.selfSigned("X509");
    assertEquals(SslOptions.X509, x509.getType());

    SslOptions pkcs12 = SslOptions.selfSigned("PKCS12");
    assertEquals(SslOptions.PKCS12, pkcs12.getType());

    assertThrows(UnsupportedOperationException.class, () -> SslOptions.selfSigned("INVALID"));
  }

  @Test
  public void clientAuth() {
    SslOptions opt = new SslOptions();
    assertEquals(SslOptions.ClientAuth.NONE, opt.getClientAuth()); // Default

    opt.setClientAuth(SslOptions.ClientAuth.REQUIRED);
    assertEquals(SslOptions.ClientAuth.REQUIRED, opt.getClientAuth());
  }

  @Test
  public void sslContext() throws Exception {
    SSLContext ctx = SSLContext.getDefault();
    SslOptions opt = new SslOptions();

    assertNull(opt.getSslContext());
    opt.setSslContext(ctx);
    assertEquals(ctx, opt.getSslContext());
  }

  @Test
  public void configParsingBranches() {
    // Tests "server.ssl" path prefix fallback and specific fields
    Config conf =
        ConfigFactory.parseMap(
            Map.of(
                "server.ssl.cert", "ssl/test.p12",
                "server.ssl.password", "changeit", // FIX: Password is required for PKCS12
                "server.ssl.clientAuth", "REQUESTED",
                "server.ssl.protocol", "TLSv1.2"));
    SslOptions opt = SslOptions.from(conf).get();

    // Implicit fallback to PKCS12 when type is missing
    assertEquals(SslOptions.PKCS12, opt.getType());
    assertEquals("changeit", opt.getPassword());
    assertEquals(SslOptions.ClientAuth.REQUESTED, opt.getClientAuth());
    assertEquals(Collections.singletonList("TLSv1.2"), opt.getProtocol());
  }

  @Test
  public void testToString() {
    assertEquals("PKCS12", new SslOptions().toString());
  }

  @Test
  public void testSetProtocolVarargs() {
    SslOptions opt = new SslOptions();
    opt.setProtocol("TLSv1.2", "TLSv1.3");
    assertEquals(Arrays.asList("TLSv1.2", "TLSv1.3"), opt.getProtocol());
  }
}
