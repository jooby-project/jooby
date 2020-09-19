package io.jooby;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jooby.internal.pac4j.UrlResolverImpl;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pac4j.Pac4jContext;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Issue1990 {

  @Test
  public void withoutContextRelative() {
    Logger log = (Logger) LoggerFactory.getLogger(UrlResolverImpl.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    log.addAppender(appender);

    UrlResolverImpl resolver = new UrlResolverImpl();

    assertEquals("/callback", resolver.compute("/callback", null));
    assertFalse(appender.list.isEmpty());

    ILoggingEvent event = appender.list.get(0);

    assertEquals("Unable to resolve URL from path '{}' since no web context was provided." +
        " This may prevent some authentication clients to work properly." +
        " Consider explicitly specifying an absolute callback URL or using a custom url resolver.", event.getMessage());

    assertEquals(Level.WARN, event.getLevel());
    assertNotNull(event.getArgumentArray());
    assertTrue(event.getArgumentArray().length > 0);
    assertEquals("/callback", event.getArgumentArray()[0]);
  }

  @Test
  public void withoutContextAbsolute() {
    Logger log = (Logger) LoggerFactory.getLogger(UrlResolverImpl.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    log.addAppender(appender);

    UrlResolverImpl resolver = new UrlResolverImpl();

    assertEquals("https://foo/bar", resolver.compute("https://foo/bar", null));
    assertEquals("http://foo/bar", resolver.compute("http://foo/bar", null));
    assertEquals("HTTPS://foo/bar", resolver.compute("HTTPS://foo/bar", null));
    assertEquals("HTTP://foo/bar", resolver.compute("HTTP://foo/bar", null));

    assertTrue(appender.list.isEmpty());
  }

  @ServerTest
  public void withContext(ServerTestRunner runner) {
    AtomicInteger port = new AtomicInteger();

    runner.define(app -> {
      UrlResolverImpl resolver = new UrlResolverImpl();
      app.get("/", ctx -> resolver.compute("/callback?q=foo", Pac4jContext.create(ctx)));
      app.onStarted(() -> port.set(app.getServerOptions().getPort()));
    }).ready(http -> {
      String portFragment = port.get() == Context.PORT ? "" : ":" + port.get();
      http.get("/", rsp -> assertEquals("http://localhost" + portFragment + "/callback", rsp.body().string()));
    });
  }
}
