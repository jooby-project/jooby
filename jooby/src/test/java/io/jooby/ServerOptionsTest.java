/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static com.typesafe.config.ConfigValueFactory.fromAnyRef;
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
  public void shouldParseFromConfig() {
    ServerOptions options =
        ServerOptions.from(
                ConfigFactory.empty()
                    .withValue("server.port", fromAnyRef(9090))
                    .withValue("server.securePort", fromAnyRef(9443))
                    .withValue("server.ioThreads", fromAnyRef(4))
                    .withValue("server.name", fromAnyRef("Test"))
                    .withValue("server.bufferSize", fromAnyRef(1024))
                    .withValue("server.defaultHeaders", fromAnyRef(false))
                    .withValue("server.compressionLevel", fromAnyRef(8))
                    .withValue("server.maxRequestSize", fromAnyRef(2048))
                    .withValue("server.workerThreads", fromAnyRef(32))
                    .withValue("server.host", fromAnyRef("0.0.0.0"))
                    .withValue("server.httpsOnly", fromAnyRef(true))
                    .resolve())
            .get();
    assertEquals(9090, options.getPort());
    assertEquals(9443, options.getSecurePort());
    assertEquals(4, options.getIoThreads());
    assertEquals("Test", options.getServer());
    assertEquals(8, options.getCompressionLevel());
    assertEquals(2048, options.getMaxRequestSize());
    assertEquals(32, options.getWorkerThreads());
    assertEquals("0.0.0.0", options.getHost());
    assertTrue(options.isHttpsOnly());
  }

  @Test
  public void shouldSetCorrectLocalHost() {
    ServerOptions options = new ServerOptions();
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("localhost");
    assertEquals("0.0.0.0", options.getHost());
    options.setHost(null);
    assertEquals("0.0.0.0", options.getHost());
    options.setHost("");
    assertEquals("0.0.0.0", options.getHost());
  }

  @Test
  @DisplayName("Test default constructor and basic accessors")
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
            entry("server.maxRequestSize", "5M"),
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
    assertEquals(5242880, opt.getMaxRequestSize()); // 5MB in bytes
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
  @DisplayName("Test getSSLContext logic branches")
  void testGetSSLContext() throws Exception {
    ServerOptions options = new ServerOptions();
    ClassLoader loader = getClass().getClassLoader();

    // 1. SSL Disabled
    assertNull(options.getSSLContext(loader));

    // 2. SSL Enabled via Secure Port, use custom SSLContext to avoid provider lookup complexity
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
