package org.jooby.internal.handlers;

import static org.easymock.EasyMock.expect;

import java.util.Optional;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Route.Definition;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.common.collect.Sets;

public class OptionsHandlerTest {

  private Block path = unit -> {
    Request req = unit.get(Request.class);
    expect(req.path()).andReturn("/");
  };

  private Block next = unit -> {
    Chain chain = unit.get(Route.Chain.class);
    chain.next(unit.get(Request.class), unit.get(Response.class));
  };

  @Test
  public void handle() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(next)
        .expect(allow(false))
        .expect(path)
        .expect(method("GET"))
        .expect(matches("POST", false))
        .expect(matches("PUT", false))
        .expect(matches("DELETE", false))
        .expect(matches("PATCH", false))
        .expect(matches("HEAD", false))
        .expect(matches("CONNECT", false))
        .expect(matches("OPTIONS", false))
        .expect(matches("TRACE", false))
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.header("Allow", "")).andReturn(rsp);
          expect(rsp.length(0)).andReturn(rsp);
          expect(rsp.status(Status.OK)).andReturn(rsp);
        })
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new OptionsHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void handleSome() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(next)
        .expect(allow(false))
        .expect(path)
        .expect(method("GET"))
        .expect(matches("POST", true))
        .expect(routeMethod("POST"))
        .expect(matches("PUT", false))
        .expect(matches("DELETE", false))
        .expect(matches("PATCH", true))
        .expect(routeMethod("PATCH"))
        .expect(matches("HEAD", false))
        .expect(matches("CONNECT", false))
        .expect(matches("OPTIONS", false))
        .expect(matches("TRACE", false))
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.header("Allow", "POST, PATCH")).andReturn(rsp);
          expect(rsp.length(0)).andReturn(rsp);
          expect(rsp.status(Status.OK)).andReturn(rsp);
        })
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new OptionsHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void handleNone() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(next)
        .expect(allow(true))
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new OptionsHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  private Block matches(final String method, final boolean matches) {
    return unit -> {
      Route route = unit.mock(Route.class);
      Optional<Route> ifRoute = matches ? Optional.of(route) : Optional.empty();
      Definition def = unit.get(Route.Definition.class);
      expect(def.matches(method, "/", MediaType.all, MediaType.ALL)).andReturn(ifRoute);
    };
  }

  private Block method(final String method) {
    return unit -> {
      Request req = unit.get(Request.class);
      expect(req.method()).andReturn(method);
    };
  }

  private Block routeMethod(final String method) {
    return unit -> {
      Route.Definition req = unit.get(Route.Definition.class);
      expect(req.method()).andReturn(method);
    };
  }

  private Block allow(final boolean set) {
    return unit -> {
      Mutant mutant = unit.mock(Mutant.class);
      expect(mutant.isSet()).andReturn(set);

      Response rsp = unit.get(Response.class);
      expect(rsp.header("Allow")).andReturn(mutant);
    };
  }

}
