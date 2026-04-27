/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Map.entry;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.net.ssl.SSLContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.jooby.output.OutputOptions;

public class ServerOptionsTest {

  @Test
  @DisplayName("Test basic accessors and boundary logic")
  void testBasicAccessors() {
    ServerOptions options = new ServerOptions();

    // Port logic
    assertEquals(ServerOptions.SERVER_PORT, options.getPort());
    options.setPort(-10);
    assertEquals(0, options.getPort(), "Port should be floored at 0");
    options.setPort(9000);
    assertEquals(9000, options.getPort());

    // IO/Worker threads
    options.setIoThreads(4);
    assertEquals(4, options.getIoThreads());
    options.setWorkerThreads(32);
    assertEquals(32, options.getWorkerThreads());

    // Host logic
    options.setHost(null);
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("   ");
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("localhost");
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("1.2.3.4");
    assertEquals("1.2.3.4", options.getHost());

    // Headers and Name
    options.setServer("Netty");
    assertEquals("Netty", options.getServer());
    options.setDefaultHeaders(false);
    assertFalse(options.getDefaultHeaders());

    // Form fields and headers
    options.setMaxFormFields(50);
    assertEquals(50, options.getMaxFormFields());
    options.setMaxHeaderSize(1024);
    assertEquals(1024, options.getMaxHeaderSize());
    options.setMaxRequestSize(2048);
    assertEquals(2048, options.getMaxRequestSize());
  }

  @Test
  @DisplayName("Test OutputOptions and Compression accessors")
  void testOutputAndCompression() {
    ServerOptions options = new ServerOptions();
    OutputOptions output = new OutputOptions();
    options.setOutput(output);
    assertEquals(output, options.getOutput());

    options.setCompressionLevel(9);
    assertEquals(9, options.getCompressionLevel());
    options.setCompressionLevel(null);
    assertNull(options.getCompressionLevel());
  }

  @Test
  @DisplayName("Test Secure Port and SSL state logic")
  void testSslState() {
    ServerOptions options = new ServerOptions();
    assertFalse(options.isSSLEnabled());

    options.setSecurePort(null);
    assertNull(options.getSecurePort());

    options.setSecurePort(443);
    assertEquals(443, options.getSecurePort());
    assertTrue(options.isSSLEnabled());

    options.setSecurePort(-1);
    assertEquals(0, options.getSecurePort());

    options = new ServerOptions();
    options.setHttpsOnly(true);
    assertTrue(options.isHttpsOnly());
    assertTrue(options.isSSLEnabled());

    options = new ServerOptions();
    options.setHttp2(true);
    assertTrue(options.isSSLEnabled());

    options.setSsl(new SslOptions());
    assertNotNull(options.getSsl());
  }

  @Test
  @DisplayName("Test Config.from - All paths")
  void testFromConfig() {
    // Test empty config
    assertFalse(ServerOptions.from(ConfigFactory.empty()).isPresent());

    // Test full config mapping
    Map<String, Object> map =
        Map.ofEntries(
            entry("server.port", 3000),
            entry("server.securePort", 3443),
            entry("server.ioThreads", 2),
            entry("server.workerThreads", 10),
            entry("server.name", "jetty"),
            entry("server.host", "127.0.0.1"),
            entry("server.defaultHeaders", false),
            entry("server.compressionLevel", 5),
            entry("server.maxRequestSize", "5M"), // Resolves to 5242880 bytes
            entry("server.maxFormFields", 200),
            entry("server.expectContinue", true),
            entry("server.httpsOnly", true),
            entry("server.http2", false),
            entry("server.output.size", 1024),
            entry("server.output.useDirectBuffers", true));

    Config config = ConfigFactory.parseMap(map);
    Optional<ServerOptions> result = ServerOptions.from(config);

    assertTrue(result.isPresent());
    ServerOptions opt = result.get();
    assertEquals(3000, opt.getPort());
    assertEquals(3443, opt.getSecurePort());
    assertEquals(2, opt.getIoThreads());
    assertEquals(10, opt.getWorkerThreads());
    assertEquals("jetty", opt.getServer());
    assertEquals("127.0.0.1", opt.getHost());
    assertFalse(opt.getDefaultHeaders());
    assertEquals(5, opt.getCompressionLevel());
    assertEquals(5242880, opt.getMaxRequestSize());
    assertEquals(200, opt.getMaxFormFields());
    assertTrue(opt.isExpectContinue());
    assertTrue(opt.isHttpsOnly());
    assertEquals(Boolean.FALSE, opt.isHttp2());
    assertEquals(1024, opt.getOutput().getSize());
    assertTrue(opt.getOutput().isDirectBuffers());
  }

  @Test
  @DisplayName("Test Unsupported server.gzip Exception")
  void testGzipException() {
    Config config =
        ConfigFactory.empty().withValue("server.gzip", ConfigValueFactory.fromAnyRef(true));
    assertThrows(UnsupportedOperationException.class, () -> ServerOptions.from(config));
  }

  @Test
  @DisplayName("Test toString() formatting and branches")
  void testToString() {
    ServerOptions options = new ServerOptions();
    options.setServer(null);
    String s1 = options.toString();
    assertTrue(s1.startsWith("server {"));
    assertFalse(s1.contains("gzip"));

    options.setServer("Netty");
    options.setCompressionLevel(1);
    String s2 = options.toString();
    assertTrue(s2.startsWith("Netty {"));
    assertTrue(s2.contains("gzip"));
  }

  @Test
  @DisplayName("Test SSLContext - Pre-configured Context")
  void testGetSSLContext_PreConfigured() throws Exception {
    ServerOptions options = new ServerOptions();
    ClassLoader loader = getClass().getClassLoader();

    // 1. SSL Disabled
    assertNull(options.getSSLContext(loader));

    // 2. SSL Enabled via Secure Port
    SSLContext mockContext = SSLContext.getDefault();
    SslOptions sslOptions = new SslOptions();
    sslOptions.setSslContext(mockContext);

    // Ensure protocol matching works (matches at least one from SslOptions defaults)
    sslOptions.setProtocol(
        Collections.singletonList(mockContext.getDefaultSSLParameters().getProtocols()[0]));

    options.setSecurePort(8443);
    options.setSsl(sslOptions);

    assertNotNull(options.getSSLContext(loader));
    assertEquals(8443, options.getSecurePort());
  }

  @Test
  @DisplayName("Test SSLContext - Missing Protocol Match")
  void testGetSSLContext_UnsupportedProtocol() throws Exception {
    ServerOptions options = new ServerOptions();
    options.setSecurePort(8443);

    SSLContext mockContext = SSLContext.getDefault();
    SslOptions sslOptions = new SslOptions();
    sslOptions.setSslContext(mockContext);

    // Set a protocol guaranteed not to be supported by default Java
    sslOptions.setProtocol(Collections.singletonList("SSLv2HelloInvalid"));
    options.setSsl(sslOptions);

    assertThrows(
        IllegalArgumentException.class, () -> options.getSSLContext(getClass().getClassLoader()));
  }

  @Test
  @DisplayName("Test SSLContext - Invalid ServiceLoader Type")
  void testGetSSLContext_InvalidType() {
    ServerOptions options = new ServerOptions();
    options.setSecurePort(8443);

    SslOptions sslOptions = new SslOptions();
    // Setting an invalid type ensures the ServiceLoader stream findFirst() fails and throws
    sslOptions.setType("INVALID_TYPE_123");
    options.setSsl(sslOptions);

    assertThrows(
        UnsupportedOperationException.class,
        () -> options.getSSLContext(getClass().getClassLoader()));
  }

  @Test
  @DisplayName("Test SSLContext - Dynamic Provider Creation")
  void testGetSSLContext_DynamicCreation() {
    ServerOptions options = new ServerOptions();
    options.setSecurePort(8443);

    // We intentionally leave SslContext null so it triggers the ServiceLoader pipeline.
    // If a provider exists on the classpath, it generates the context. If not, it throws.
    // Catching the exception ensures branch coverage succeeds regardless of classpath state.
    try {
      SSLContext ctx = options.getSSLContext(getClass().getClassLoader());
      assertNotNull(ctx);
    } catch (UnsupportedOperationException ex) {
      assertTrue(ex.getMessage().contains("SSL Type"));
    }
  }

  @Test
  @DisplayName("Test ExpectContinue accessor")
  void testExpectContinue() {
    ServerOptions options = new ServerOptions();
    assertNull(options.isExpectContinue());
    options.setExpectContinue(true);
    assertTrue(options.isExpectContinue());
  }

  @Test
  @DisplayName("Test package-private constructor")
  void testPackagePrivateConstructor() {
    ServerOptions options = new ServerOptions(true);
    assertTrue(options.defaults);
  }
}
