package org.jooby.internal.pac4j;

import static org.easymock.EasyMock.expect;

import org.jooby.Err;
import org.jooby.Response;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.pac4j.core.client.Client;
import org.pac4j.core.exception.RequiresHttpAction;
import org.pac4j.http.client.indirect.IndirectBasicAuthClient;

public class AuthResponseTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Response.class)
        .run(unit -> {
          new AuthResponse(unit.get(Response.class));
        });
  }

  @Test
  public void committed() throws Exception {
    new MockUnit(Response.class, Client.class, RequiresHttpAction.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(true);
        })
        .run(unit -> {
          new AuthResponse(unit.get(Response.class))
              .handle(unit.get(Client.class), unit.get(RequiresHttpAction.class));
        });
  }

  @Test
  public void noerr() throws Exception {
    new MockUnit(Response.class, Client.class, RequiresHttpAction.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(false);

          RequiresHttpAction req = unit.get(RequiresHttpAction.class);
          expect(req.getCode()).andReturn(302);
        })
        .run(unit -> {
          new AuthResponse(unit.get(Response.class))
              .handle(unit.get(Client.class), unit.get(RequiresHttpAction.class));
        });
  }

  @Test(expected = Err.class)
  public void err() throws Exception {
    new MockUnit(Response.class, Client.class, RequiresHttpAction.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(false);

          RequiresHttpAction req = unit.get(RequiresHttpAction.class);
          expect(req.getCode()).andReturn(401);
        })
        .run(unit -> {
          new AuthResponse(unit.get(Response.class))
              .handle(unit.get(Client.class), unit.get(RequiresHttpAction.class));
        });
  }

  public void statusCode() throws Exception {
    new MockUnit(Response.class, IndirectBasicAuthClient.class, RequiresHttpAction.class)
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.committed()).andReturn(false);
          expect(rsp.status(401)).andReturn(rsp);
          rsp.end();

          RequiresHttpAction req = unit.get(RequiresHttpAction.class);
          expect(req.getCode()).andReturn(401);
        })
        .run(unit -> {
          new AuthResponse(unit.get(Response.class))
              .handle(unit.get(IndirectBasicAuthClient.class), unit.get(RequiresHttpAction.class));
        });
  }

}
