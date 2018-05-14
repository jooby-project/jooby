package org.jooby.metrics;

import static org.easymock.EasyMock.expect;

import java.util.Optional;
import java.util.SortedMap;

import org.jooby.Mutant;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.codahale.metrics.health.HealthCheck.Result;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableSortedMap;

public class HealthCheckHandlerTest {

  private Block registry = unit -> {
    Request request = unit.get(Request.class);
    expect(request.require(HealthCheckRegistry.class))
        .andReturn(unit.get(HealthCheckRegistry.class));
  };

  private Block param(final String name, final Optional<String> value) {
    return unit -> {
      Request req = unit.get(Request.class);
      Mutant mvalue = unit.mock(Mutant.class);
      expect(mvalue.toOptional()).andReturn(value);
      expect(req.param(name)).andReturn(mvalue);
    };
  }

  @Test
  public void notImplemented() throws Exception {
    SortedMap<String, Result> checks = ImmutableSortedMap.of();
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.empty()))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthChecks()).andReturn(checks);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.NOT_IMPLEMENTED)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void allchecks() throws Exception {
    SortedMap<String, Result> checks = ImmutableSortedMap.of("c1", Result.healthy(), "c2",
        Result.healthy());
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.empty()))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthChecks()).andReturn(checks);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void allchecksErrOnFirst() throws Exception {
    SortedMap<String, Result> checks = ImmutableSortedMap.of(
        "c1", Result.unhealthy("intentional err"),
        "c2", Result.healthy());
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.empty()))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthChecks()).andReturn(checks);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.SERVER_ERROR)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void allchecksErrOnLast() throws Exception {
    SortedMap<String, Result> checks = ImmutableSortedMap.of("c1", Result.healthy(), "c2",
        Result.unhealthy("intentional err"));
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.empty()))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthChecks()).andReturn(checks);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.SERVER_ERROR)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void allchecksErrOnMiddle() throws Exception {
    SortedMap<String, Result> checks = ImmutableSortedMap.of("c1", Result.healthy(), "c2",
        Result.unhealthy("intentional err"), "c3", Result.healthy());
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.empty()))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthChecks()).andReturn(checks);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.SERVER_ERROR)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void somechecks() throws Exception {
    Result check = Result.healthy();
    SortedMap<String, Result> checks = ImmutableSortedMap.of("c", check);
    new MockUnit(Request.class, Response.class, HealthCheckRegistry.class)
        .expect(registry)
        .expect(param("name", Optional.of("c")))
        .expect(unit -> {
          HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
          expect(registry.runHealthCheck("c")).andReturn(check);
        })
        .expect(unit -> {
          Response rsp = unit.get(Response.class);
          expect(rsp.status(Status.OK)).andReturn(rsp);
          expect(rsp.header("Cache-Control", "must-revalidate,no-cache,no-store")).andReturn(rsp);
          rsp.send(checks);
        })
        .run(unit -> {
          new HealthCheckHandler().handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

}
