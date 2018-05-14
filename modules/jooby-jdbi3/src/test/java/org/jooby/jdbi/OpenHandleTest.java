package org.jooby.jdbi;

import com.google.inject.Key;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jdbi.v3.core.Handle;
import org.jdbi.v3.core.Jdbi;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.test.MockUnit;
import org.junit.Test;

import java.util.Optional;

public class OpenHandleTest {
  @Test
  public void openHandle() throws Exception {
    new MockUnit(Handle.class, Request.class, Response.class, Route.Chain.class, Jdbi.class, Handle.class)
        .expect(unit -> {
          Handle handle = unit.get(Handle.class);
          expect(handle.setReadOnly(true)).andReturn(handle);
          expect(handle.begin()).andReturn(handle);

          Request request = unit.get(Request.class);
          expect(request.set(Key.get(Handle.class), handle)).andReturn(request);

          Response response = unit.get(Response.class);
          response.after(isA(CommitTransaction.class));
          response.complete(isA(RollbackTransaction.class));
          response.complete(isA(CloseHandle.class));

          Route.Chain chain = unit.get(Route.Chain.class);
          chain.next(request, response);

          Jdbi jdbi = unit.get(Jdbi.class);
          expect(jdbi.open()).andReturn(handle);
        })
        .run(unit -> {
          TransactionalRequest req = new TransactionalRequest();
          req.doWith(h -> {
            h.setReadOnly(true);
          });
          new OpenHandle(unit.get(Jdbi.class), req)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

}
