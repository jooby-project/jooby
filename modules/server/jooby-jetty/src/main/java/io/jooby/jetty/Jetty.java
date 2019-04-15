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

import io.jooby.ExecutionMode;
import io.jooby.Jooby;
import io.jooby.ServerOptions;
import io.jooby.internal.jetty.JettyHandler;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.MultiPartFormDataCompliance;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.gzip.GzipHandler;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import io.jooby.Throwing;

import javax.annotation.Nonnull;
import java.net.BindException;
import java.util.ArrayList;
import java.util.List;

/**
 * Web server implementation using <a href="https://www.eclipse.org/jetty/">Jetty</a>.
 *
 * @author edgar
 * @since 2.0.0
 */
public class Jetty extends io.jooby.Server.Base {

  private static final int THREADS = 200;

  private Server server;

  private List<Jooby> applications = new ArrayList<>();

  private ServerOptions options = new ServerOptions()
      .setServer("jetty")
      .setWorkerThreads(THREADS);

  static {
    System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.Slf4jLog");
  }

  @Nonnull @Override public Jetty setOptions(@Nonnull ServerOptions options) {
    this.options = options
        .setWorkerThreads(options.getWorkerThreads(THREADS));
    return this;
  }

  @Nonnull @Override public ServerOptions getOptions() {
    return options;
  }

  @Nonnull @Override public io.jooby.Server start(Jooby application) {
    try {
      System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset", "utf-8");
      /** Set max request size attribute: */
      System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
          Long.toString(options.getMaxRequestSize()));

      /** Jetty only support worker executor: */
      application.setExecutionMode(ExecutionMode.WORKER);
      applications.add(application);

      addShutdownHook();

      QueuedThreadPool executor = new QueuedThreadPool(options.getWorkerThreads());
      executor.setName("application");

      fireStart(applications, executor);

      this.server = new Server(executor);
      server.setStopAtShutdown(false);

      HttpConfiguration httpConf = new HttpConfiguration();
      httpConf.setOutputBufferSize(options.getBufferSize());
      httpConf.setOutputAggregationSize(options.getBufferSize());
      httpConf.setSendXPoweredBy(false);
      httpConf.setSendDateHeader(options.isDefaultHeaders());
      httpConf.setSendServerVersion(false);
      httpConf.setMultiPartFormDataCompliance(MultiPartFormDataCompliance.RFC7578);
      ServerConnector connector = new ServerConnector(server);
      connector.addConnectionFactory(new HttpConnectionFactory(httpConf));
      connector.setPort(options.getPort());
      connector.setHost("0.0.0.0");

      server.addConnector(connector);

      AbstractHandler handler = new JettyHandler(applications.get(0), options.getBufferSize(),
          options.getMaxRequestSize(), options.isDefaultHeaders());

      if (options.isGzip()) {
        GzipHandler gzipHandler = new GzipHandler();
        gzipHandler.setHandler(handler);
        handler = gzipHandler;
      }

      server.setHandler(handler);

      server.start();

      fireReady(applications);
    } catch (Exception x) {
      if (x.getCause() instanceof BindException) {
        x = new BindException("Address already in use: " + options.getPort());
      }
      throw Throwing.sneakyThrow(x);
    }

    return this;
  }

  @Nonnull @Override public io.jooby.Server stop() {
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
