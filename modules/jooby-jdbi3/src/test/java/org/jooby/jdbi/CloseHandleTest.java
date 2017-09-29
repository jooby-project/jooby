package org.jooby.jdbi;

import static org.easymock.EasyMock.expect;
import org.jdbi.v3.core.Handle;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;

import java.util.Optional;

public class CloseHandleTest {
  @Test
  public void closeHandle() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isClosed(false))
        .expect(unit -> {
          Handle handle = unit.get(Handle.class);
          handle.close();
        })
        .run(unit -> {
          new CloseHandle(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Optional.empty());
        });
  }

  @Test
  public void closeHandleIsClosed() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isClosed(true))
        .run(unit -> {
          new CloseHandle(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Optional.empty());
        });
  }

  private MockUnit.Block isClosed(final boolean closed) {
    return unit -> {
      Handle handle = unit.get(Handle.class);
      expect(handle.isClosed()).andReturn(closed);
    };
  }
}
