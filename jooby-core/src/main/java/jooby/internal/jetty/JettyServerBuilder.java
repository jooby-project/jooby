package jooby.internal.jetty;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jooby.WebSocket;
import jooby.WebSocket.Definition;
import jooby.internal.RouteHandler;
import jooby.internal.WebSocketImpl;

import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.HttpConnectionFactory;
import org.eclipse.jetty.server.SecureRequestCustomizer;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.server.SslConnectionFactory;
import org.eclipse.jetty.server.handler.ContextHandler;
import org.eclipse.jetty.util.component.AbstractLifeCycle.AbstractLifeCycleListener;
import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.util.resource.Resource;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.WebSocketBehavior;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.server.WebSocketServerFactory;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeRequest;
import org.eclipse.jetty.websocket.servlet.ServletUpgradeResponse;
import org.eclipse.jetty.websocket.servlet.WebSocketCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class JettyServerBuilder {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(jooby.Server.class);
  protected static final String WebSocketImpl = null;

  public static Server build(final Config config, final RouteHandler routeHandler)
      throws Exception {
    // jetty URL charset
    System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset",
        config.getString("application.charset"));

    Server server = new Server();

    Config $ = config.getConfig("jetty");

    HttpConfiguration httpConfig = configure(new HttpConfiguration(), $);

    httpConfig.setSecurePort(config.getInt("application.securePort"));
    httpConfig.setSecureScheme("https");

    // HTTP connector
    ServerConnector http = configure(new ServerConnector(server, new HttpConnectionFactory(
        httpConfig)), $.getConfig("http"));
    http.setPort(config.getInt("application.port"));

    server.addConnector(http);

    String keystorePath = config.getString("ssl.keystore.path");
    Resource keystore = Resource.newClassPathResource(keystorePath);
    if (keystore == null || !keystore.exists()) {
      keystore = Resource.newResource(keystorePath);
    }

    if (keystore.exists()) {
      SslContextFactory sslContextFactory = new SslContextFactory();
      sslContextFactory.setKeyStoreResource(keystore);
      sslContextFactory.setKeyStorePassword(config.getString("ssl.keystore.password"));
      sslContextFactory.setKeyManagerPassword(config.getString("ssl.keymanager.password"));

      // get config from https if present and/or fallback to http
      HttpConfiguration httpsConfig = new HttpConfiguration(httpConfig);
      httpsConfig.addCustomizer(new SecureRequestCustomizer());

      ServerConnector https = configure(new ServerConnector(server,
          new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
          new HttpConnectionFactory(httpsConfig)),
          $.getConfig("https"));
      https.setPort(config.getInt("application.securePort"));

      server.addConnector(https);
    }

    // contextPath
    String contextPath = config.getString("application.path");

    server.setStopAtShutdown(true);

    ContextHandler context = new ContextHandler(contextPath);
    JettyHandler handler = new JettyHandler(routeHandler, config);

    Injector injector = routeHandler.injector();
    @SuppressWarnings("unchecked")
    Set<WebSocket.Definition> sockets = (Set<Definition>) injector.getInstance(Key.get(Types
        .setOf(WebSocket.Definition.class)));

    if (sockets.size() > 0) {
      WebSocketPolicy policy = configure(new WebSocketPolicy(WebSocketBehavior.SERVER),
          config.getConfig("jetty.ws.policy"));
      WebSocketServerFactory socketServerFactory = new WebSocketServerFactory(policy);
      socketServerFactory.setCreator(new WebSocketCreator() {
        @Override
        public Object createWebSocket(final ServletUpgradeRequest req,
            final ServletUpgradeResponse resp) {
          String path = req.getRequestPath();
          for (WebSocket.Definition socketDef : sockets) {
            Optional<WebSocket> matches = socketDef.matches(path);
            if (matches.isPresent()) {
              WebSocketImpl socket = (WebSocketImpl) matches.get();
              return new JettyWebSocketHandler(injector, config, socket);
            }
          }
          return null;
        }
      });

      handler.addBean(socketServerFactory);
    }

    context.setHandler(handler);

    server.setHandler(context);

    server.addLifeCycleListener(new AbstractLifeCycleListener() {
      @Override
      public void lifeCycleStarted(final LifeCycle event) {
        log.info("\nRoutes:\n{}", routeHandler);
      }
    });

    return server;
  }

  private static <T> T configure(final T target, final Config config) {
    Class<?> clazz = target.getClass();
    Map<String, Method> methods = Arrays.stream(clazz.getMethods())
        .filter(m -> m.getName().startsWith("set") && m.getParameterCount() == 1)
        .collect(Collectors.toMap(m -> m.getName(), m -> m));
    for (Entry<String, ConfigValue> entry : config.entrySet()) {
      String name = entry.getKey();
      if (name.contains(".")) {
        // a sub tree, just ignore it
        continue;
      }
      String methodName = "set" + Character.toUpperCase(name.charAt(0)) + name.substring(1);
      try {
        Method method = methods.get(methodName);
        checkArgument(method != null, "Unknown property: jetty.%s", name);
        Class<?> paramType = method.getParameterTypes()[0];
        final Object value;
        if (paramType == int.class) {
          value = config.getInt(name);
        } else if (paramType == long.class) {
          value = config.getLong(name);
        } else if (paramType == boolean.class) {
          value = config.getBoolean(name);
        } else {
          value = config.getString(name);
        }
        method.invoke(target, value);
      } catch (IllegalAccessException | InvocationTargetException ex) {
        throw new IllegalArgumentException("Bad property value jetty." + name, ex);
      }
    }
    return target;
  }

}
