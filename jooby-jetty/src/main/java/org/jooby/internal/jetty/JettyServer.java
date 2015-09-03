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
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
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

public class JettyServer implements org.jooby.spi.Server {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(org.jooby.spi.Server.class);

  private Server server;

  @Inject
  public JettyServer(final HttpHandler handler, final Config config) {
    this.server = server(handler, config);
  }

  private Server server(final HttpHandler handler, final Config config) {
    System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset",
        config.getString("jetty.url.charset"));

    System.setProperty("org.eclipse.jetty.server.Request.maxFormContentSize",
        config.getBytes("server.http.MaxRequestSize").toString());

    QueuedThreadPool pool = conf(new QueuedThreadPool(), config.getConfig("jetty.threads"),
        "jetty.threads");

    Server server = new Server(pool);
    server.setStopAtShutdown(false);

    // HTTP connector
    ServerConnector http = connector(server, config.getConfig("jetty.http"), "jetty.http");
    http.setPort(config.getInt("application.port"));
    http.setHost(config.getString("application.host"));

    server.addConnector(http);

    WebSocketPolicy wsConfig = conf(new WebSocketPolicy(WebSocketBehavior.SERVER),
        config.getConfig("jetty.ws"), "jetty.ws");
    WebSocketServerFactory webSocketServerFactory = new WebSocketServerFactory(wsConfig);
    webSocketServerFactory.setCreator((req, rsp) -> {
      JettyWebSocket ws = new JettyWebSocket();
      req.getHttpServletRequest().setAttribute(JettyWebSocket.class.getName(), ws);
      return ws;
    });

    server.setHandler(new JettyHandler(handler, webSocketServerFactory, config
        .getString("application.tmpdir")));

    return server;
  }

  private ServerConnector connector(final Server server, final Config conf, final String path) {
    HttpConfiguration httpConfig = conf(new HttpConfiguration(), conf.withoutPath("connector"),
        path);

    HttpConnectionFactory httpFactory = new HttpConnectionFactory(httpConfig);

    ServerConnector connector = new ServerConnector(server, httpFactory);

    return conf(connector, conf.getConfig("connector"), path + ".connector");
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

  private void tryOption(final Object source, final Config config, final Method option) {
    try {
      String optionName = option.getName().replace("set", "");
      Object optionValue = config.getAnyRef(optionName);
      Class<?> optionType = Primitives.wrap(option.getParameterTypes()[0]);
      if (Number.class.isAssignableFrom(optionType)) {
        if (optionValue instanceof String) {
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
      }
      log.debug("{}.{}({})", source.getClass().getSimpleName(), option.getName(), optionValue);
      option.invoke(source, optionValue);
    } catch (Exception ex) {
      Throwable cause = ex;
      if (ex instanceof InvocationTargetException) {
        cause = ((InvocationTargetException) ex).getTargetException();
      }
      log.error("invocation of " + option + " resulted in exception", cause);
      throw Throwables.propagate(cause);
    }
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
