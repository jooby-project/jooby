package org.jooby.metrics;

import static org.easymock.EasyMock.expect;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.junit.Test;

public class PingHandlerTest {

  @Test
  public void ping() throws Exception {
    new MockUnit(Request.class, Response.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.plain)).andReturn(rsp);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send("pong");
        })
        .run(unit -> {
          new PingHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

}
