package jooby.internal.jetty;

import static com.google.common.base.Preconditions.checkArgument;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import jooby.internal.RouteHandler;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigValue;

public class JettyServerBuilder {

  /** The logging system. */
  private static final Logger log = LoggerFactory.getLogger(jooby.Server.class);

  public static Server build(final Config config, final RouteHandler routeHandler)
      throws Exception {
    // jetty URL charset
    System.setProperty("org.eclipse.jetty.util.UrlEncoded.charset",
        config.getString("application.charset"));

    Server server = new Server();

    Config $ = config.getConfig("jetty");

    HttpConfiguration httpConfig = new HttpConfiguration();
    dump(httpConfig, $);
    httpConfig.setSecurePort(config.getInt("application.securePort"));
    httpConfig.setSecureScheme("https");

    // HTTP connector
    ServerConnector http = new ServerConnector(server, new HttpConnectionFactory(httpConfig));
    dump(http, $.getConfig("http"));
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

      ServerConnector https = new ServerConnector(server,
          new SslConnectionFactory(sslContextFactory, "HTTP/1.1"),
          new HttpConnectionFactory(httpsConfig));
      dump(https, $.getConfig("https"));
      https.setPort(config.getInt("application.securePort"));

      server.addConnector(https);
    }

    // contextPath
    String contextPath = config.getString("application.path");
    // work dir
    String workDir = config.getString("java.io.tmpdir");

    server.setStopAtShutdown(true);

    ContextHandler handler = new ContextHandler(contextPath);
    handler.setHandler(new JettyHandler(routeHandler, workDir));

    server.setHandler(handler);

    server.addLifeCycleListener(new AbstractLifeCycleListener() {
      @Override
      public void lifeCycleStarted(final LifeCycle event) {
        log.info("Routes:\n{}", routeHandler);
      }
    });

    return server;
  }

  private static <T> void dump(final T target, final Config config) {
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
  }

}
