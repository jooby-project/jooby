/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import com.typesafe.config.Config;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Optional;

/**
 * Available server options.
 *
 * @author edgar
 * @since 2.0.0
 */
public class ServerOptions {

  /** Default application port <code>8080</code> or the value of system property <code>server.port</code>. */
  public static final int DEFAULT_PORT = Integer
      .parseInt(System.getProperty("server.port", "8080"));

  /** 4kb constant in bytes. */
  public static final int _4KB = 4096;

  /** 8kb constant in bytes. */
  public static final int _8KB = 8192;

  /** 16kb constant in bytes. */
  public static final int _16KB = 16384;

  /** 10mb constant in bytes. */
  public static final int _10MB = 20971520;

  /** Buffer size used by server. Usually for reading/writing data. */
  private int bufferSize = _16KB;

  /** Number of available threads, but never smaller than <code>2</code>. */
  public static final int IO_THREADS = Runtime.getRuntime().availableProcessors() * 2;

  /** Number of worker (a.k.a application) threads. It is the number of processors multiply by
   * <code>8</code>. */
  public static final int WORKER_THREADS = IO_THREADS * 8;

  /** HTTP port. Default is <code>8080</code> or <code>0</code> for random port. */
  private int port = DEFAULT_PORT;

  /** Number of IO threads used by the server. Required by Netty and Undertow. */
  private Integer ioThreads;

  /** Number of worker threads (a.k.a application) to use. */
  private Integer workerThreads;

  /** Enabled gzip at server level. Default is: <code>false</code>. */
  private boolean gzip;

  /**
   * Configure server to default headers: <code>Date</code>, <code>Content-Type</code> and
   * <code>Server</code> header.
   */
  private boolean defaultHeaders = true;

  /**
   * Indicates if the web server should use a single loop/group for doing IO or not. Netty only.
   */
  private Boolean singleLoop;

  /** Name of server: Jetty, Netty or Utow. */
  private String server;

  /**
   * Maximum request size in bytes. Request exceeding this value results in
   * {@link io.jooby.StatusCode#REQUEST_ENTITY_TOO_LARGE} response. Default is <code>10mb</code>.
   */
  private int maxRequestSize = _10MB;

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
      if (conf.hasPath("server.singleLoop")) {
        options.setSingleLoop(conf.getBoolean("server.singleLoop"));
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
      if (conf.hasPath("server.defaultHeaders")) {
        options.setDefaultHeaders(conf.getBoolean("server.defaultHeaders"));
      }
      if (conf.hasPath("server.maxRequestSize")) {
        options.setMaxRequestSize((int) conf.getMemorySize("server.maxRequestSize").toBytes());
      }
      if (conf.hasPath("server.workerThreads")) {
        options.setWorkerThreads(conf.getInt("server.workerThreads"));
      }
      return Optional.of(options);
    }
    return Optional.empty();
  }

  @Override public String toString() {
    StringBuilder buff = new StringBuilder();
    buff.append(server).append(" {");
    buff.append("port: ").append(port);
    if (!"jetty".equals(server) && ioThreads != null) {
      buff.append(", ioThreads: ").append(ioThreads);
    }
    buff.append(", workerThreads: ").append(getWorkerThreads());
    if ("netty".equals(server)) {
      buff.append(", singleLoop: ").append(singleLoop);
    }
    buff.append(", bufferSize: ").append(bufferSize);
    buff.append(", maxRequestSize: ").append(maxRequestSize);
    if (gzip) {
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
   */
  public boolean isGzip() {
    return gzip;
  }

  /**
   * Enabled/disabled gzip.
   *
   * @param gzip True to enabled it. Default is disabled(false).
   * @return This options.
   */
  public @Nonnull ServerOptions setGzip(boolean gzip) {
    this.gzip = gzip;
    return this;
  }

  /**
   * True if default headers: <code>Date</code>, <code>Content-Type</code> and <code>Server</code>
   * are enabled.
   *
   * @return True if default headers: <code>Date</code>, <code>Content-Type</code> and
   * <code>Server</code> are enabled.
   */
  public boolean isDefaultHeaders() {
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
   * Whenever the underlying server uses a single loop/group for accepting and processing
   * connections or not. Netty only.
   * @return True for use a single loop group for accepting and processing connections.
   */
  public boolean getSingleLoop() {
    return getSingleLoop(true);
  }

  /**
   * Whenever the underlying server uses a single loop/group for accepting and processing
   * connections or not. Netty only.
   *
   * @param defaultValue Default value if none is was set.
   * @return True for use a single loop group for accepting and processing connections.
   */
  public boolean getSingleLoop(boolean defaultValue) {
    return singleLoop == null ? defaultValue : singleLoop.booleanValue();
  }

  /**
   * Indicates if netty server uses a single loop for accepting and processing connections.
   * Netty only, default is <code>false</code>.
   *
   * @param singleLoop True for single loop.
   * @return This options.
   */
  public @Nonnull ServerOptions setSingleLoop(boolean singleLoop) {
    this.singleLoop = singleLoop;
    return this;
  }

  private int randomPort() {
    try (ServerSocket socket = new ServerSocket(0)) {
      return socket.getLocalPort();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
