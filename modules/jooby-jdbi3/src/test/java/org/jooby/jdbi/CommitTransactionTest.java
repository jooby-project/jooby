package org.jooby.jdbi;

import static org.easymock.EasyMock.expect;
import org.jdbi.v3.core.Handle;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Results;
import org.jooby.test.MockUnit;
import org.junit.Test;

import java.util.Optional;

public class CommitTransactionTest {
  @Test
  public void commitTransaction() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isInTransaction(true))
        .expect(unit -> {
          Handle handle = unit.get(Handle.class);
          expect(handle.commit()).andReturn(handle);
        })
        .run(unit -> {
          new CommitTransaction(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Results.ok());
        });
  }

  @Test
  public void inactiveTransaction() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class)
        .expect(isInTransaction(false))
        .run(unit -> {
          new CommitTransaction(unit.get(Handle.class))
              .handle(unit.get(Request.class), unit.get(Response.class), Results.ok());
        });
  }

  private MockUnit.Block isInTransaction(final boolean isInTransaction) {
    return unit -> {
      Handle handle = unit.get(Handle.class);
      expect(handle.isInTransaction()).andReturn(isInTransaction);
    };
  }
}
