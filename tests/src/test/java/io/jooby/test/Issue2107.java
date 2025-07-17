/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.test;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.jooby.Jooby;
import io.jooby.exception.StartupException;

public class Issue2107 {

  @Test
  public void appInitExceptionLogging() {
    Throwable t =
        assertThrows(
            StartupException.class,
            () -> Jooby.runApp(new String[0], AppWithRuntimeException::new));
    assertEquals("Application initialization resulted in exception", t.getMessage());
    assertNotNull(t.getCause());
    assertInstanceOf(RuntimeException.class, t.getCause());
    assertEquals("meh", t.getCause().getMessage());
  }

  @Test
  public void noMatryoshkaExceptionsPlease() {
    Throwable t =
        assertThrows(
            StartupException.class,
            () -> Jooby.runApp(new String[0], AppWithStartupException::new));
    assertEquals("meh", t.getMessage());
    assertNull(t.getCause());
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
