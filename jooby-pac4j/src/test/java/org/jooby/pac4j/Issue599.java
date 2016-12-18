package org.jooby.pac4j;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.Optional;

import org.jooby.Err;
import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.internal.pac4j.AuthContext;
import org.jooby.internal.pac4j.AuthSerializer;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AuthContext.class, AuthSerializer.class })
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
    new MockUnit(Request.class, Response.class, Mutant.class)
        .expect(params)
        .expect(unit -> {
          Request req = unit.get(Request.class);
          expect(req.secure()).andReturn(false);
          expect(req.hostname()).andReturn("localhost");
          expect(req.port()).andReturn(8080);
          expect(req.path()).andReturn("/foo");
          expect(req.queryString()).andReturn(Optional.of("bar=1"));
        })
        .run(unit -> {
          AuthContext ctx = new AuthContext(unit.get(Request.class), unit.get(Response.class));
          assertEquals("http://localhost:8080/foo?bar=1", ctx.getFullRequestURL());
        });
  }

}
