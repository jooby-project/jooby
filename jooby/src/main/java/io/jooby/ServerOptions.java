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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.ServerSocket;

public class ServerOptions {

  public static final int _4KB = 4096;

  public static final int _8KB = 8192;

  /** 16KB constant. */
  public static final int _16KB = 16384;

  public static final int _10MB = 20971520;

  private int bufferSize = _16KB;

  public static final int IO_THREADS = Math.max(Runtime.getRuntime().availableProcessors(), 2);

  public static final int WORKER_THREADS = IO_THREADS * 8;

  private int port = 8080;

  private Integer ioThreads;

  private Integer workerThreads;

  private boolean gzip;

  private boolean defaultHeaders = true;

  private Boolean singleLoop;

  private String server;

  private int maxRequestSize = _10MB;

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

  public @Nonnull ServerOptions setServer(@Nonnull String server) {
    this.server = server;
    return this;
  }

  public @Nonnull String getServer() {
    return server;
  }

  public int getPort() {
    return port;
  }

  public @Nonnull ServerOptions setPort(int port) {
    this.port = port == 0 ? randomPort() : port;
    return this;
  }

  public int getIoThreads() {
    return getIoThreads(IO_THREADS);
  }

  public int getIoThreads(int defaultIoThreads) {
    return ioThreads == null ? defaultIoThreads : ioThreads;
  }

  public @Nonnull ServerOptions setIoThreads(int ioThreads) {
    this.ioThreads = ioThreads;
    return this;
  }

  public int getWorkerThreads() {
    return getWorkerThreads(WORKER_THREADS);
  }

  public int getWorkerThreads(int defaultWorkerThreads) {
    return workerThreads == null ? defaultWorkerThreads : workerThreads;
  }

  public @Nonnull ServerOptions setWorkerThreads(int workerThreads) {
    this.workerThreads = workerThreads;
    return this;
  }

  public boolean isGzip() {
    return gzip;
  }

  public @Nonnull ServerOptions setGzip(boolean gzip) {
    this.gzip = gzip;
    return this;
  }

  public boolean isDefaultHeaders() {
    return defaultHeaders;
  }

  public @Nonnull ServerOptions setDefaultHeaders(boolean defaultHeaders) {
    this.defaultHeaders = defaultHeaders;
    return this;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public @Nonnull ServerOptions setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  public int getMaxRequestSize() {
    return maxRequestSize;
  }

  public @Nonnull ServerOptions setMaxRequestSize(int maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    return this;
  }

  public boolean getSingleLoop() {
    return getSingleLoop(true);
  }

  public boolean getSingleLoop(boolean defaultValue) {
    return singleLoop == null ? defaultValue : singleLoop.booleanValue();
  }

  public @Nonnull ServerOptions setSingleLoop(boolean singleLoop) {
    this.singleLoop = singleLoop;
    return this;
  }

  private int randomPort() {
    try (ServerSocket socket = new ServerSocket()) {
      return socket.getLocalPort();
    } catch (IOException x) {
      throw Throwing.sneakyThrow(x);
    }
  }
}
