/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jooby.Jooby;
import io.jooby.exception.StartupException;

public class Issue2107 {

  @Test
  public void appInitExceptionLogging() {
    withLog(
        appender -> {
          Throwable t =
              assertThrows(
                  StartupException.class,
                  () -> Jooby.runApp(new String[0], AppWithRuntimeException::new));
          assertEquals("Application initialization resulted in exception", t.getMessage());
          assertNotNull(t.getCause());
          assertTrue(t.getCause() instanceof RuntimeException);
          assertEquals("meh", t.getCause().getMessage());
          assertEquals(1, appender.list.size());

          final ILoggingEvent ev = appender.list.get(0);
          assertEquals(Level.ERROR, ev.getLevel());
          assertEquals("Application initialization resulted in exception", ev.getMessage());
          assertEquals("meh", ev.getThrowableProxy().getMessage());
        });
  }

  @Test
  public void noMatryoshkaExceptionsPlease() {
    withLog(
        appender -> {
          Throwable t =
              assertThrows(
                  StartupException.class,
                  () -> Jooby.runApp(new String[0], AppWithStartupException::new));
          assertEquals("meh", t.getMessage());
          assertNull(t.getCause());
          assertEquals(1, appender.list.size());

          final ILoggingEvent ev = appender.list.get(0);
          assertEquals(Level.ERROR, ev.getLevel());
          assertEquals("Application initialization resulted in exception", ev.getMessage());
          assertEquals("meh", ev.getThrowableProxy().getMessage());
        });
  }

  static class AppWithRuntimeException extends Jooby {

    {
      if (true) throw new RuntimeException("meh");
    }
  }

  static class AppWithStartupException extends Jooby {

    {
      if (true) throw new StartupException("meh");
    }
  }

  private void withLog(Consumer<ListAppender<ILoggingEvent>> consumer) {
    Logger log = (Logger) LoggerFactory.getLogger(Jooby.class);
    var appender = new ListAppender<ILoggingEvent>();
    try {
      appender.start();
      log.addAppender(appender);
      log.setAdditive(false);
      consumer.accept(appender);
    } finally {
      log.detachAppender(appender);
      log.setAdditive(true);
    }
  }
}
