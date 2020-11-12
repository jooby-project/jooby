package io.jooby;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import io.jooby.exception.StartupException;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class Issue2107 {

  @Test
  public void appInitExceptionLogging() {
    Logger log = (Logger) LoggerFactory.getLogger(Jooby.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    log.addAppender(appender);

    Throwable t = assertThrows(StartupException.class, () -> Jooby.runApp(new String[0], AppWithRuntimeException::new));
    assertEquals("Application initialization resulted in exception", t.getMessage());
    assertNotNull(t.getCause());
    assertTrue(t.getCause() instanceof RuntimeException);
    assertEquals("meh", t.getCause().getMessage());
    assertEquals(1, appender.list.size());

    final ILoggingEvent ev = appender.list.get(0);
    assertEquals(Level.ERROR, ev.getLevel());
    assertEquals("Application initialization resulted in exception", ev.getMessage());
    assertEquals("meh", ev.getThrowableProxy().getMessage());
  }

  @Test
  public void noMatryoshkaExceptionsPlease() {
    Logger log = (Logger) LoggerFactory.getLogger(Jooby.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    log.addAppender(appender);

    Throwable t = assertThrows(StartupException.class, () -> Jooby.runApp(new String[0], AppWithStartupException::new));
    assertEquals("meh", t.getMessage());
    assertNull(t.getCause());
    assertEquals(1, appender.list.size());

    final ILoggingEvent ev = appender.list.get(0);
    assertEquals(Level.ERROR, ev.getLevel());
    assertEquals("Application initialization resulted in exception", ev.getMessage());
    assertEquals("meh", ev.getThrowableProxy().getMessage());
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
}
