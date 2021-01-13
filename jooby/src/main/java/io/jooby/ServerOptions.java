/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

import java.io.IOException;
import java.net.ServerSocket;
import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Stream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.net.ssl.SSLContext;

import com.typesafe.config.Config;
import io.jooby.internal.SslContextProvider;

/**
 * Available server options.
 *
 * @author edgar
 * @since 2.0.0
 */
public class ServerOptions {

  /** Default application port <code>8080</code> or the value of system property <code>server.port</code>. */
  public static final int SERVER_PORT = Integer
      .parseInt(System.getProperty("server.port", "8080"));

  /**
   * Default application secure port <code>8443</code> or the value of system property
   * <code>server.securePort</code>.
   */
  public static final int SEVER_SECURE_PORT = Integer
      .parseInt(System.getProperty("server.securePort", "8443"));

  /**  Default compression level for gzip. */
  public static final int DEFAULT_COMPRESSION_LEVEL = 6;

  /** 4kb constant in bytes. */
  public static final int _4KB = 4096;

  /** 8kb constant in bytes. */
  public static final int _8KB = 8192;

  /** 16kb constant in bytes. */
  public static final int _16KB = 16384;

  /** 10mb constant in bytes. */
  public static final int _10MB = 10485760;

  /** Buffer size used by server. Usually for reading/writing data. */
  private int bufferSize = _16KB;

  /** Number of available threads, but never smaller than <code>2</code>. */
  public static final int IO_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  /** Number of worker (a.k.a application) threads. It is the number of processors multiply by
   * <code>8</code>. */
  public static final int WORKER_THREADS = Runtime.getRuntime().availableProcessors() * 8;

  /** HTTP port. Default is <code>8080</code> or <code>0</code> for random port. */
  private int port = SERVER_PORT;

  /** Number of IO threads used by the server. Used by Netty and Undertow. */
  private Integer ioThreads;

  /** Number of worker threads (a.k.a application) to use. */
  private Integer workerThreads;

  /**
   * Configure server to default headers: <code>Date</code>, <code>Content-Type</code> and
   * <code>Server</code> header.
   */
  private boolean defaultHeaders = true;

  /** Name of server: Jetty, Netty or Utow. */
  private String server;

  /**
   * Maximum request size in bytes. Request exceeding this value results in
   * {@link io.jooby.StatusCode#REQUEST_ENTITY_TOO_LARGE} response. Default is <code>10mb</code>.
   */
  private int maxRequestSize = _10MB;

  private String host = "0.0.0.0";

  private SslOptions ssl;

  private Integer securePort;

  private Integer compressionLevel;

  private Boolean http2;

  /**
   * Creates server options from config object. The configuration options must provided entries
   * like: <code>server.port</code>, <code>server.ioThreads</code>, etc...
   *
   * @param conf Configuration object.
   * @return Server options.
   */
  public static @Nonnull Optional<ServerOptions> from(@Nonnull Config conf) {
    if (conf.hasPath("server")) {
      ServerOptions options = new ServerOptions();
      if (conf.hasPath("server.port")) {
        options.setPort(conf.getInt("server.port"));
      }
      if (conf.hasPath("server.securePort")) {
        options.setSecurePort(conf.getInt("server.securePort"));
      }
      if (conf.hasPath("server.ioThreads")) {
        options.setIoThreads(conf.getInt("server.ioThreads"));
      }
      if (conf.hasPath("server.name")) {
        options.setServer(conf.getString("server.name"));
      }
      if (conf.hasPath("server.bufferSize")) {
        options.setBufferSize(conf.getInt("server.bufferSize"));
      }
      if (conf.hasPath("server.defaultHeaders")) {
        options.setDefaultHeaders(conf.getBoolean("server.defaultHeaders"));
      }
      if (conf.hasPath("server.gzip")) {
        options.setGzip(conf.getBoolean("server.gzip"));
      }
      if (conf.hasPath("server.compressionLevel")) {
        options.setCompressionLevel(conf.getInt("server.compressionLevel"));
      }
      if (conf.hasPath("server.maxRequestSize")) {
        options.setMaxRequestSize((int) conf.getMemorySize("server.maxRequestSize").toBytes());
      }
      if (conf.hasPath("server.workerThreads")) {
        options.setWorkerThreads(conf.getInt("server.workerThreads"));
      }
      if (conf.hasPath("server.host")) {
        options.setHost(conf.getString("server.host"));
      }
      // ssl
      SslOptions.from(conf, "server.ssl").ifPresent(options::setSsl);
      if (conf.hasPath("server.http2")) {
        options.setHttp2(conf.getBoolean("server.http2"));
      }

      return Optional.of(options);
    }
    return Optional.empty();
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(Optional.ofNullable(server).orElse("server")).append(" {");
    buff.append("port: ").append(port);
    if (!"jetty".equals(server)) {
      buff.append(", ioThreads: ").append(Optional.ofNullable(ioThreads).orElse(IO_THREADS));
    }
    buff.append(", workerThreads: ").append(getWorkerThreads());
    buff.append(", bufferSize: ").append(bufferSize);
    buff.append(", maxRequestSize: ").append(maxRequestSize);
    if (compressionLevel != null) {
      buff.append(", gzip");
    }
    buff.append("}");

    return buff.toString();
  }

  /**
   * Set server name.
   *
   * @param server Name of the underlying server.
   * @return This options.
   */
  public @Nonnull ServerOptions setServer(@Nonnull String server) {
    this.server = server;
    return this;
  }

  /**
   * Server name.
   *
   * @return Server name.
   */
  public @Nonnull String getServer() {
    return server;
  }

  /**
   * Server HTTP port.
   *
   * @return Server HTTP port.
   */
  public int getPort() {
    return port;
  }

  /**
   * Set the server port (default is 8080). For random port use: <code>0</code>.
   *
   * @param port Server port or <code>0</code> to pick a random port.
   * @return This options.
   */
  public @Nonnull ServerOptions setPort(int port) {
    this.port = port == 0 ? randomPort() : port;
    return this;
  }

  /**
   * HTTPs port or <code>null</code>.
   *
   * @return HTTPs port or <code>null</code>.
   */
  public @Nullable Integer getSecurePort() {
    return securePort;
  }

  /**
   * True when SSL is enabled. Either bc the secure port or SSL options are set.
   *
   * @return True when SSL is enabled. Either bc the secure port or SSL options are set.
   */
  public boolean isSSLEnabled() {
    return securePort != null || ssl != null;
  }

  /**
   * Set HTTPs port.
   *
   * @param securePort Port number or <code>0</code> for random number.
   * @return This options.
   */
  public @Nullable ServerOptions setSecurePort(@Nullable Integer securePort) {
    if (securePort == null) {
      this.securePort = null;
    } else {
      this.securePort = securePort.intValue() == 0 ? randomPort() : securePort.intValue();
    }
    return this;
  }

  /**
   * Number of IO threads used by the server. Required by Netty and Undertow.
   *
   * @return Number of IO threads used by the server. Required by Netty and Undertow.
   */
  public int getIoThreads() {
    return getIoThreads(IO_THREADS);
  }

  /**
   * Number of IO threads used by the server. Required by Netty and Undertow.
   *
   * @param defaultIoThreads Default number of threads if none was set.
   * @return Number of IO threads used by the server. Required by Netty and Undertow.
   */
  public int getIoThreads(int defaultIoThreads) {
    return ioThreads == null ? defaultIoThreads : ioThreads;
  }

  /**
   * Set number of IO threads to use. Supported by Netty and Undertow.
   *
   * @param ioThreads Number of threads. Must be greater than <code>0</code>.
   * @return This options.
   */
  public @Nonnull ServerOptions setIoThreads(int ioThreads) {
    this.ioThreads = ioThreads;
    return this;
  }

  /**
   * Number of worker threads (a.k.a application) to use. This are the threads which are allowed to
   * block.
   *
   * @return Number of worker threads (a.k.a application) to use. This are the threads which are
   *   allowed to block.
   */
  public int getWorkerThreads() {
    return getWorkerThreads(WORKER_THREADS);
  }

  /**
   * Number of worker threads (a.k.a application) to use. This are the threads which are allowed to
   * block.
   *
   * @param defaultWorkerThreads Default worker threads is none was set.
   * @return Number of worker threads (a.k.a application) to use. This are the threads which are
   *   allowed to block.
   */
  public int getWorkerThreads(int defaultWorkerThreads) {
    return workerThreads == null ? defaultWorkerThreads : workerThreads;
  }

  /**
   * Set number of worker threads (a.k.a application) to use. This are the threads which are
   * allowed to block.
   *
   * @param workerThreads Number of worker threads to use.
   * @return This options.
   */
  public @Nonnull ServerOptions setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
    return this;
  }

  /**
   * True if gzip is enabled.
   *
   * @return True if gzip is enabled.
   * @deprecated In favor of {@link #getCompressionLevel()}.
   */
  @Deprecated
  public boolean getGzip() {
    return compressionLevel != null;
  }

  /**
   * Enabled/disabled gzip.
   *
   * @param gzip True to enabled it. Default is disabled(false).
   * @return This options.
   * @deprecated In favor of {@link #setCompressionLevel(Integer)}.
   */
  @Deprecated
  public @Nonnull ServerOptions setGzip(boolean gzip) {
    if (gzip) {
      this.compressionLevel = DEFAULT_COMPRESSION_LEVEL;
    } else {
      this.compressionLevel = null;
    }
    return this;
  }

  /**
   * Indicates compression level to use while producing gzip responses.
   *
   *
   * @return Compression level value between <code>0...9</code> or <code>null</code> when off.
   */
  public @Nullable Integer getCompressionLevel() {
    return compressionLevel;
  }

  /**
   * Set compression level to use while producing gzip responses.
   *
   * Gzip is off by default (compression level is null).
   *
   * @param compressionLevel Value between <code>0..9</code> or <code>null</code>.
   * @return This options.
   */
  public @Nonnull ServerOptions setCompressionLevel(@Nullable Integer compressionLevel) {
    this.compressionLevel = compressionLevel;
    return this;
  }

  /**
   * True if default headers: <code>Date</code>, <code>Content-Type</code> and <code>Server</code>
   * are enabled.
   *
   * @return True if default headers: <code>Date</code>, <code>Content-Type</code> and
   * <code>Server</code> are enabled.
   */
  public boolean getDefaultHeaders() {
    return defaultHeaders;
  }

  /**
   * Enabled/disabled default server headers: <code>Date</code>, <code>Content-Type</code> and
   * <code>Server</code>. Enabled by default.
   *
   * @param defaultHeaders True for enabled.
   * @return This options.
   */
  public @Nonnull ServerOptions setDefaultHeaders(boolean defaultHeaders) {
    this.defaultHeaders = defaultHeaders;
    return this;
  }

  /**
   * Server buffer size in bytes. Default is: <code>16kb</code>. Used for reading/writing data.
   *
   * @return Server buffer size in bytes. Default is: <code>16kb</code>. Used for reading/writing
   *    data.
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * Set buffer size.
   *
   * @param bufferSize Buffer size.
   * @return This options.
   */
  public @Nonnull ServerOptions setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  /**
   * Maximum request size in bytes. Request exceeding this value results in
   * {@link io.jooby.StatusCode#REQUEST_ENTITY_TOO_LARGE} response. Default is <code>10mb</code>.
   *
   * @return Maximum request size in bytes.
   */
  public int getMaxRequestSize() {
    return maxRequestSize;
  }

  /**
   * Set max request size in bytes.
   *
   * @param maxRequestSize Max request size in bytes.
   * @return This options.
   */
  public @Nonnull ServerOptions setMaxRequestSize(int maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    return this;
  }

  /**
   * Server host, defaults is <code>0.0.0.0</code>.
   *
   * @return Server host, defaults is <code>0.0.0.0</code>.
   */
  public String getHost() {
    return host;
  }

  /**
   * Set the server host, defaults to <code>0.0.0.0</code>.
   *
   * @param host Server host. Localhost, null or empty values fallback to <code>0.0.0.0</code>.
   */
  public void setHost(String host) {
    if (host == null || host.trim().length() == 0 || "localhost".equalsIgnoreCase(host.trim())) {
      this.host = "0.0.0";
    } else {
      this.host = host;
    }
  }

  /**
   * SSL options.
   *
   * @return SSL options.
   */
  public @Nullable SslOptions getSsl() {
    return ssl;
  }

  /**
   * Set SSL options.
   *
   * @param ssl SSL options.
   * @return Server options.
   */
  public @Nonnull ServerOptions setSsl(@Nullable SslOptions ssl) {
    this.ssl = ssl;
    return this;
  }

  /**
   * Specify when HTTP/2 is enabled or not. This value is set to <code>null</code>, which allows
   * Jooby to enabled by default when dependency is added it.
   *
   * To turn off set to false.
   *
   * @return Whenever HTTP/2 is enabled.
   */
  public @Nullable Boolean isHttp2() {
    return http2;
  }

  /**
   * Turn on/off HTTP/2 support.
   *
   * @param http2 True to enabled.
   * @return This options.
   */
  public ServerOptions setHttp2(@Nullable Boolean http2) {
    this.http2 = http2;
    return this;
  }

  /**
   * Creates SSL context using the given resource loader. This method attempts to create a
   * SSLContext when:
   *
   * - {@link #getSecurePort()} has been set; or
   * - {@link #getSsl()} has been set.
   *
   * If secure port is set and there is no SSL options, this method configure a SSL context using
   * the a self-signed certificate for <code>localhost</code>.
   *
   * @param loader Resource loader.
   * @return SSLContext or <code>null</code> when SSL is disabled.
   */
  public @Nullable SSLContext getSSLContext(@Nonnull ClassLoader loader) {
    if (isSSLEnabled()) {
      setSecurePort(Optional.ofNullable(securePort).orElse(SEVER_SECURE_PORT));
      setSsl(Optional.ofNullable(ssl).orElseGet(SslOptions::selfSigned));
      SslOptions options = getSsl();

      SslContextProvider sslContextProvider = Stream.of(SslContextProvider.providers())
          .filter(it -> it.supports(options.getType()))
          .findFirst()
          .orElseThrow(
              () -> new UnsupportedOperationException("SSL Type: " + options.getType()));

      String providerName = stream(spliteratorUnknownSize(
          ServiceLoader.load(SslProvider.class).iterator(),
          Spliterator.ORDERED),
          false
      )
          .findFirst()
          .map(provider -> {
            String name = provider.getName();
            if (Security.getProvider(name) == null) {
              Security.addProvider(provider.create());
            }
            return name;
          })
          .orElse(null);

      SSLContext sslContext = sslContextProvider.create(loader, providerName, options);
      // validate TLS protocol, at least one protocol must be supported
      Set<String> supportedProtocols = new LinkedHashSet<>(Arrays
          .asList(sslContext.getDefaultSSLParameters().getProtocols()));
      Set<String> protocols = new LinkedHashSet<>(options.getProtocol());
      protocols.retainAll(supportedProtocols);
      if (protocols.isEmpty()) {
        throw new IllegalArgumentException("Unsupported protocol: " + options.getProtocol());
      }
      ssl.setProtocol(new ArrayList<>(protocols));
      return sslContext;
    }
    return null;
  }

  private static int randomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException x) {
      throw SneakyThrows.propagate(x);
    }
  }
}
