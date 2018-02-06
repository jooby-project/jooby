package org.jooby.internal;

import org.jooby.internal.parser.ParserExecutor;
import org.jooby.test.MockUnit;
import org.junit.Test;

/**
 * TODO: complete unit tests.
 */
public class SessionImplTest {

  @Test
  public void renewIdShouldDoNothing() throws Exception {
    new MockUnit(ParserExecutor.class)
        .run(unit -> {
          new SessionImpl(unit.get(ParserExecutor.class), true, "sid", 0L)
              .renewId();
        });
  }
}
