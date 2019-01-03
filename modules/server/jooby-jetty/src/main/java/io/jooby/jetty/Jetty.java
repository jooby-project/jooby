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
package io.jooby.jetty;

import io.jooby.Jooby;
import io.jooby.internal.jetty.JettyHandler;
import io.jooby.internal.jetty.JettyMultiHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.util.thread.ScheduledExecutorScheduler;
import org.eclipse.jetty.util.thread.Scheduler;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Jetty extends io.jooby.Server.Base {

  private int port = 8080;

  private Server server;

  private boolean gzip;

  private List<Jooby> applications = new ArrayList<>();

  private long maxRequestSize = _10MB;

  @Override public io.jooby.Server port(int port) {
    this.port = port;
    return this;
  }

  @Override public int port() {
    return port;
  }

  @Nonnull @Override public io.jooby.Server maxRequestSize(long maxRequestSize) {
    this.maxRequestSize = maxRequestSize;
    return this;
  }

  public io.jooby.Server gzip(boolean enabled) {
    this.gzip = enabled;
    return this;
  }

  @Nonnull @Override public io.jooby.Server deploy(Jooby application) {
    applications.add(application);
    return this;
  }

  @Nonnull @Override public io.jooby.Server start() {
    System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset", "utf-8");
    /** Set max request size attribute: */
    System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
        Long.toString(maxRequestSize));

    addShutdownHook();

    QueuedThreadPool executor = new QueuedThreadPool(64);
    executor.setName("jetty");

    fireStart(applications, () -> executor);

    this.server = new Server(executor);
    server.setStopAtShutdown(false);

    HttpConfiguration httpConf = new HttpConfiguration();
    httpConf.setOutputBufferSize(_16KB);
    httpConf.setOutputAggregationSize(_16KB);
    httpConf.setSendXPoweredBy(false);
    httpConf.setSendDateHeader(false);
    httpConf.setSendServerVersion(false);
    httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
    int acceptors = 1;
    int selectors = Runtime.getRuntime().availableProcessors();
    Scheduler scheduler = new ScheduledExecutorScheduler("jetty-scheduler", false);
    ServerConnector connector = new ServerConnector(server, executor, scheduler, null,
        acceptors, selectors, new HttpConnectionFactory(httpConf));
    connector.setPort(port);
    connector.setHost("0.0.0.0");

    server.addConnector(connector);

    AbstractHandler handler = applications.size() == 1 ?
        new JettyHandler(applications.get(0)) :
        new JettyMultiHandler(applications);

    if (gzip) {
      GzipHandler gzipHandler = new GzipHandler();
      gzipHandler.setHandler(handler);
      handler = gzipHandler;
    }

    server.setHandler(handler);

    try {
      server.start();
    } catch (Exception x) {
      throw Throwing.sneakyThrow(x);
    }

    fireReady(applications);

    return this;
  }

  @Override public io.jooby.Server stop() {
    fireStop(applications);
    if (server != null) {
      try {
        server.stop();
      } catch (Exception x) {
        throw Throwing.sneakyThrow(x);
      }
      server = null;
    }
    return this;
  }
}
