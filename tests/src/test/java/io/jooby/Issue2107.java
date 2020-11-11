package io.jooby;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class Issue2107 {

  @Test
  public void appInitExceptionLogging() {
    Logger log = (Logger) LoggerFactory.getLogger(Jooby.class);
    ListAppender<ILoggingEvent> appender = new ListAppender<>();
    appender.start();
    log.addAppender(appender);

    Throwable t = assertThrows(RuntimeException.class, () -> Jooby.runApp(new String[0], App::new));
    assertEquals("meh", t.getMessage());
    assertEquals(1, appender.list.size());

    final ILoggingEvent ev = appender.list.get(0);
    assertEquals(Level.ERROR, ev.getLevel());
    assertEquals("Application initialization resulted in exception", ev.getMessage());
    assertEquals("meh", ev.getThrowableProxy().getMessage());
  }

  static class App extends Jooby {

    {
      if (true) throw new RuntimeException("meh");
    }
  }
}
