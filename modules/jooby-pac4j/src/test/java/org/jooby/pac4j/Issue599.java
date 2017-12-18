package org.jooby.pac4j;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.jooby.*;
import org.jooby.internal.pac4j2.AuthContext;
import org.jooby.internal.pac4j2.AuthSerializer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.pac4j.core.context.session.SessionStore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Map;
import java.util.Optional;

import static org.easymock.EasyMock.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthContext.class, AuthSerializer.class})
public class Issue599 {

  private Block params = unit -> {
    Mutant param = unit.get(Mutant.class);
    expect(param.toList()).andReturn(ImmutableList.of("v1"));
    expect(param.toList()).andThrow(new Err(Status.BAD_REQUEST));

    Map<String, Mutant> map = ImmutableMap.of("p1", param, "p2", param);

    Mutant params = unit.mock(Mutant.class);
    expect(params.toMap()).andReturn(map);

    Request req = unit.get(Request.class);
    expect(req.params()).andReturn(params);
  };

  @Test
  public void shouldKeepQueryString() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class, SessionStore.class)
        .expect(params)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.hostname()).andReturn("localhost");
          expect(req.port()).andReturn(8080);
          expect(req.path()).andReturn("/foo");
          expect(req.contextPath()).andReturn("");
          expect(req.queryString()).andReturn(Optional.of("bar=1"));
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class), unit.get(SessionStore.class));
          assertEquals("http://localhost:8080/foo?bar=1", ctx.getFullRequestURL());
        });
  }

  @Test
  public void shouldKeepQueryStringWithContextpath() throws Exception {
    new MockUnit(Request.class, Response.class, Mutant.class, SessionStore.class)
        .expect(params)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.hostname()).andReturn("localhost");
          expect(req.port()).andReturn(8080);
          expect(req.path()).andReturn("/foo");
          expect(req.contextPath()).andReturn("/cpath");
          expect(req.queryString()).andReturn(Optional.of("bar=1"));
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class), unit.get(SessionStore.class));
          assertEquals("http://localhost:8080/cpath/foo?bar=1", ctx.getFullRequestURL());
        });
  }

}
