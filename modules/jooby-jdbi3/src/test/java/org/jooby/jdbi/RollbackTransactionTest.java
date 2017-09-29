package org.jooby.jdbi;

import static org.easymock.EasyMock.expect;
import org.jdbi.v3.core.Handle;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;

import java.util.Optional;

public class RollbackTransactionTest {
  @Test
  public void rollbackHandle() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isInTransaction(true))
        .expect(unit -> {
          Handle handle = unit.get(Handle.class);
          expect(handle.rollback()).andReturn(handle);
        })
        .run(unit -> {
          new RollbackTransaction(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Optional.of(new Throwable()));
        });
  }

  @Test
  public void inactiveTransaction() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isInTransaction(false))
        .run(unit -> {
          new RollbackTransaction(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Optional.of(new Throwable()));
        });
  }

  private MockUnit.Block isInTransaction(final boolean isInTransaction) {
    return unit -> {
      Handle handle = unit.get(Handle.class);
      expect(handle.isInTransaction()).andReturn(isInTransaction);
    };
  }
}
