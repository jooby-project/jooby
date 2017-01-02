/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.internal.jetty;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.net.ssl.SSLContext;

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2CServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.jooby.spi.HttpHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.primitives.Primitives;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;

import javaslang.control.Try;

public class JettyServer implements org.jooby.spi.Server {

  private static final String H2 = "h2";
  private static final String H2_17 = "h2-17";
  private static final String HTTP_1_1 = "http/1.1";

  private static final String JETTY_HTTP = "jetty.http";
  private static final String CONNECTOR = "connector";

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(org.jooby.spi.Server.class);

  private Server server;
  private Executor executor;

  @Inject
  public JettyServer(final HttpHandler handler, final Config conf,
      final Provider<SSLContext> sslCtx) {
    this.server = server(handler, conf, sslCtx);
    this.executor = this.server.getThreadPool();
  }

  private Server server(final HttpHandler handler, final Config conf,
      final Provider<SSLContext> sslCtx) {
    System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset",
        conf.getString("jetty.url.charset"));

    System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
        conf.getBytes("server.http.MaxRequestSize").toString());

    QueuedThreadPool pool = conf(new QueuedThreadPool(), conf.getConfig("jetty.threads"),
        "jetty.threads");

    Server server = new Server(pool);
    server.setStopAtShutdown(false);

    // HTTP connector
    boolean http2 = conf.getBoolean("server.http2.enabled");

    ServerConnector http = http(server, conf.getConfig(JETTY_HTTP), JETTY_HTTP, http2);
    http.setPort(conf.getInt("application.port"));
    http.setHost(conf.getString("application.host"));

    if (conf.hasPath("application.securePort")) {

      ServerConnector https = https(server, conf.getConfig(JETTY_HTTP), JETTY_HTTP,
          sslCtx.get(), http2);
      https.setPort(conf.getInt("application.securePort"));

      server.addConnector(https);
    }

    server.addConnector(http);

    WebSocketPolicy wsConfig = conf(new WebSocketPolicy(WebSocketBehavior.SERVER),
        conf.getConfig("jetty.ws"), "jetty.ws");
    WebSocketServerFactory webSocketServerFactory = new WebSocketServerFactory(wsConfig);
    webSocketServerFactory.setCreator((req, rsp) -> {
      JettyWebSocket ws = new JettyWebSocket();
      req.getHttpServletRequest().setAttribute(JettyWebSocket.class.getName(), ws);
      return ws;
    });

    ContextHandler sch = new ContextHandler();
    // always '/' context path is internally handle by jooby
    sch.setContextPath("/");
    sch.setHandler(new JettyHandler(handler, webSocketServerFactory, conf
        .getString("application.tmpdir"), conf.getBytes("jetty.FileSizeThreshold").intValue()));

    server.setHandler(sch);

    return server;
  }

  private ServerConnector http(final Server server, final Config conf, final String path,
      final boolean http2) {
    HttpConfiguration httpConfig = conf(new HttpConfiguration(), conf.withoutPath(CONNECTOR),
        path);

    ServerConnector connector;
    if (http2) {
      connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig),
          new HTTP2CServerConnectionFactory(httpConfig));
    } else {
      connector = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    }

    return conf(connector, conf.getConfig(CONNECTOR), path + "." + CONNECTOR);
  }

  private ServerConnector https(final Server server, final Config conf, final String path,
      final SSLContext sslContext, final boolean http2) {

    HttpConfiguration httpConf = conf(new HttpConfiguration(), conf.withoutPath(CONNECTOR),
        path);

    SslContextFactory sslContextFactory = new SslContextFactory();
    sslContextFactory.setSslContext(sslContext);
    sslContextFactory.setIncludeProtocols("TLSv1.2");
    sslContextFactory.setIncludeCipherSuites("TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256");

    HttpConfiguration httpsConf = new HttpConfiguration(httpConf);
    httpsConf.addCustomizer(new SecureRequestCustomizer());

    HttpConnectionFactory https11 = new HttpConnectionFactory(httpsConf);

    if (http2) {
      ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory(H2, H2_17, HTTP_1_1);
      alpn.setDefaultProtocol(HTTP_1_1);

      HTTP2ServerConnectionFactory https2 = new HTTP2ServerConnectionFactory(httpsConf);

      ServerConnector connector = new ServerConnector(server,
          new SslConnectionFactory(sslContextFactory, "alpn"), alpn, https2, https11);

      return conf(connector, conf.getConfig(CONNECTOR), path + ".connector");
    } else {

      ServerConnector connector = new ServerConnector(server,
          new SslConnectionFactory(sslContextFactory, HTTP_1_1), https11);

      return conf(connector, conf.getConfig(CONNECTOR), path + ".connector");
    }
  }

  @Override
  public void start() throws Exception {
    server.start();
  }

  @Override
  public void join() throws InterruptedException {
    server.join();
  }

  @Override
  public void stop() throws Exception {
    server.stop();
  }

  @Override
  public Optional<Executor> executor()
  {
    return Optional.of(executor);
  }

  private void tryOption(final Object source, final Config config, final Method option) {
    Try.run(() -> {
      String optionName = option.getName().replace("set", "");
      Object optionValue = config.getAnyRef(optionName);
      Class<?> optionType = Primitives.wrap(option.getParameterTypes()[0]);
      if (Number.class.isAssignableFrom(optionType) && optionValue instanceof String) {
        // either a byte or time unit
        try {
          optionValue = config.getBytes(optionName);
        } catch (ConfigException.BadValue ex) {
          optionValue = config.getDuration(optionName, TimeUnit.MILLISECONDS);
        }
        if (optionType == Integer.class) {
          // to int
          optionValue = ((Number) optionValue).intValue();
        }
      }
      log.debug("{}.{}({})", source.getClass().getSimpleName(), option.getName(), optionValue);
      option.invoke(source, optionValue);
    }).onFailure(x -> {
      Throwable cause = x;
      if (x instanceof InvocationTargetException) {
        cause = ((InvocationTargetException) x).getTargetException();
      }
      Throwables.propagate(cause);
    });
  }

  private <T> T conf(final T source, final Config config, final String path) {
    Map<String, Method> methods = Arrays.stream(source.getClass().getMethods())
        .filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1)
        .collect(Collectors.toMap(Method::getName, Function.<Method> identity()));

    config.entrySet().forEach(entry -> {
      String key = "set" + entry.getKey();
      Method method = methods.get(key);
      if (method != null) {
        tryOption(source, config, method);
      } else {
        log.error("Unknown option: {}.{} for: {}", path, key, source.getClass().getName());
      }
    });

    return source;
  }

}
