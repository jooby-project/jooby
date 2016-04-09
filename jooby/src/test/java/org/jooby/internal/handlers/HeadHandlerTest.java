package org.jooby.internal.handlers;

import static org.easymock.EasyMock.expect;

import java.util.Optional;
import java.util.Set;

import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Route.Chain;
import org.jooby.Route.Definition;
import org.jooby.internal.RouteImpl;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.google.common.collect.Sets;

public class HeadHandlerTest {

  private Block path = unit -> {
    Request req = unit.get(Request.class);
    expect(req.path()).andReturn("/");
  };

  private Block len = unit -> {
    Response rsp = unit.get(Response.class);
    expect(rsp.length(0)).andReturn(rsp);
  };

  private Block next = unit -> {
    Chain chain = unit.get(Route.Chain.class);
    chain.next(unit.get(Request.class), unit.get(Response.class));
  };

  @Test
  public void handle() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(path)
        .expect(unit -> {
          Route.Definition routeDef = unit.get(Route.Definition.class);
          expect(routeDef.glob()).andReturn(false);

          RouteImpl route = unit.mock(RouteImpl.class);
          route.handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));

          Optional<Route> ifRoute = Optional.of(route);

          expect(routeDef.matches(Route.GET, "/", MediaType.all, MediaType.ALL)).andReturn(ifRoute);
        })
        .expect(len)
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new HeadHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void noRoute() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(path)
        .expect(unit -> {
          Route.Definition routeDef = unit.get(Route.Definition.class);
          expect(routeDef.glob()).andReturn(false);

          Optional<Route> ifRoute = Optional.empty();

          expect(routeDef.matches(Route.GET, "/", MediaType.all, MediaType.ALL)).andReturn(ifRoute);
        })
        .expect(next)
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new HeadHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void ignoreGlob() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(path)
        .expect(unit -> {
          Route.Definition routeDef = unit.get(Route.Definition.class);
          expect(routeDef.glob()).andReturn(true);
        })
        .expect(next)
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet(unit.get(Route.Definition.class));
          new HeadHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

  @Test
  public void noroutes() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, Route.Definition.class)
        .expect(path)
        .expect(next)
        .run(unit -> {
          Set<Definition> routes = Sets.newHashSet();
          new HeadHandler(routes)
              .handle(unit.get(Request.class), unit.get(Response.class),
                  unit.get(Route.Chain.class));
        });
  }

}
