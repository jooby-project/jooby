package org.jooby.metrics;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.io.IOException;
import java.util.Optional;

import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.Status;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

public class InstrumentedHandlerTest {

  private Block capture = unit -> {
    Response rsp = unit.get(Response.class);
    rsp.complete(unit.capture(Route.Complete.class));
  };

  private Block onComplete = unit -> {
    unit.captured(Route.Complete.class).iterator().next()
        .handle(unit.get(Request.class), unit.get(Response.class), Optional.empty());
  };

  private Block registry = unit -> {
    Request request = unit.get(Request.class);
    expect(request.require(MetricRegistry.class))
        .andReturn(unit.get(MetricRegistry.class));
  };

  private Block counter = unit -> {
    Counter counter = unit.mock(Counter.class);
    counter.inc();
    counter.dec();

    MetricRegistry registry = unit.get(MetricRegistry.class);
    expect(registry.counter("request.actives")).andReturn(counter);
  };

  private Block timer = unit -> {
    Context ctx = unit.mock(Context.class);
    expect(ctx.stop()).andReturn(1L);

    Timer timer = unit.mock(Timer.class);
    expect(timer.time()).andReturn(ctx);

    MetricRegistry registry = unit.get(MetricRegistry.class);
    expect(registry.timer("request")).andReturn(timer);
  };

  private Block meter = unit -> {
    Meter meter = unit.mock(Meter.class);
    meter.mark();

    MetricRegistry registry = unit.get(MetricRegistry.class);
    expect(registry.meter("responses.200")).andReturn(meter);

    Response rsp = unit.get(Response.class);
    expect(rsp.status()).andReturn(Optional.of(Status.OK));
  };

  private Block next = unit -> {
    Request req = unit.get(Request.class);
    Response rsp = unit.get(Response.class);
    Route.Chain chain = unit.get(Route.Chain.class);
    chain.next(req, rsp);
  };

  private Block nextErr = unit -> {
    Request req = unit.get(Request.class);
    Response rsp = unit.get(Response.class);
    Route.Chain chain = unit.get(Route.Chain.class);
    chain.next(req, rsp);
    expectLastCall().andThrow(new IOException());
  };

  @Test
  public void instrument() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, MetricRegistry.class)
        .expect(registry)
        .expect(counter)
        .expect(timer)
        .expect(capture)
        .expect(meter)
        .expect(next)
        .run(unit -> {
          new InstrumentedHandler().handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        }, onComplete);
  }

  @Test(expected = IOException.class)
  public void instrumentWithErr() throws Exception {
    new MockUnit(Request.class, Response.class, Route.Chain.class, MetricRegistry.class)
        .expect(registry)
        .expect(counter)
        .expect(timer)
        .expect(capture)
        .expect(meter)
        .expect(nextErr)
        .run(unit -> {
          new InstrumentedHandler().handle(unit.get(Request.class), unit.get(Response.class),
              unit.get(Route.Chain.class));
        }, onComplete);
  }

}
