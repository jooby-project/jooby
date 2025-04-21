/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jooby.Context;
import io.jooby.internal.pac4j.UrlResolverImpl;
import io.jooby.junit.ServerTest;
import io.jooby.junit.ServerTestRunner;
import io.jooby.pac4j.Pac4jContext;

public class Issue1990 {

  @Test
  public void withoutContextRelative() {
    var event =
        withLogback(
            new UrlResolverImpl(),
            resolver -> {
              assertEquals("/callback", resolver.compute("/callback", null));
            });
    assertTrue(event.isPresent());

    var e = event.get();

    assertEquals(
        "Unable to resolve URL from path '{}' since no web context was provided. This may prevent"
            + " some authentication clients to work properly. Consider explicitly specifying an"
            + " absolute callback URL or using a custom url resolver.",
        e.getMessage());

    assertEquals(Level.WARN, e.getLevel());
    var args = e.getArgumentArray();
    assertNotNull(args);
    assertTrue(args.length > 0);
    assertEquals("/callback", args[0]);
  }

  @Test
  public void withoutContextAbsolute() {
    var event =
        withLogback(
            new UrlResolverImpl(),
            resolver -> {
              assertEquals("https://foo/bar", resolver.compute("https://foo/bar", null));
              assertEquals("http://foo/bar", resolver.compute("http://foo/bar", null));
              assertEquals("HTTPS://foo/bar", resolver.compute("HTTPS://foo/bar", null));
              assertEquals("HTTP://foo/bar", resolver.compute("HTTP://foo/bar", null));
            });
    assertTrue(event.isEmpty());
  }

  @ServerTest
  public void withContext(ServerTestRunner runner) {
    runner
        .define(
            app -> {
              UrlResolverImpl resolver = new UrlResolverImpl();
              app.get("/", ctx -> resolver.compute("/callback?q=foo", Pac4jContext.create(ctx)));
            })
        .ready(
            http -> {
              var port = runner.getAllocatedPort();
              String portFragment = port == Context.PORT ? "" : ":" + port;
              http.get(
                  "/",
                  rsp ->
                      assertEquals(
                          "http://localhost" + portFragment + "/callback", rsp.body().string()));
            });
  }

  private Optional<ILoggingEvent> withLogback(
      UrlResolverImpl resolver, Consumer<UrlResolverImpl> consumer) {
    var log = (Logger) LoggerFactory.getLogger(resolver.getClass());
    // MUST RUN ON INFO LEVEL
    log.setLevel(Level.INFO);
    var appender = new ListAppender<ILoggingEvent>();
    try {
      appender.start();
      log.addAppender(appender);
      consumer.accept(resolver);
      return appender.list.stream().findFirst();
    } finally {
      appender.stop();
    }
  }
}
