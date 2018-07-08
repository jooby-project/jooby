package org.jooby.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import static org.easymock.EasyMock.expect;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Route;
import org.jooby.internal.micrometer.TimedSupport;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Optional;

@RunWith(PowerMockRunner.class)
@PrepareForTest({TimedHandler.class, TimedSupport.class})
public class TimedHandlerTest {

  @Test
  public void skipHandle() throws Exception {
    new MockUnit(Request.class, Response.class, Route.class)
        .expect(unit -> {
          Route route = unit.get(Route.class);

          Request request = unit.get(Request.class);
          expect(request.route()).andReturn(route);

          unit.mockStatic(TimedSupport.class);
          expect(TimedSupport.create(route)).andReturn(null);
        })
        .run(unit -> {
          new TimedHandler()
              .handle(unit.get(Request.class), unit.get(Response.class));
        });
  }

  @Test
  public void handle() throws Exception {
    new MockUnit(Request.class, Response.class, Route.class, TimedSupport.class,
        MeterRegistry.class, TimedSupport.Sample.class)
        .expect(unit -> {
          MeterRegistry registry = unit.get(MeterRegistry.class);

          Route route = unit.get(Route.class);

          Request request = unit.get(Request.class);
          expect(request.route()).andReturn(route);
          expect(request.require(MeterRegistry.class)).andReturn(registry);

          TimedSupport.Sample sample = unit.get(TimedSupport.Sample.class);
          expect(sample.stop()).andReturn(1L);

          TimedSupport timer = unit.get(TimedSupport.class);
          expect(timer.start(registry)).andReturn(sample);

          unit.mockStatic(TimedSupport.class);
          expect(TimedSupport.create(route)).andReturn(timer);

          Response rsp = unit.get(Response.class);
          rsp.complete(unit.capture(Route.Complete.class));
        })
        .run(unit -> {
          new TimedHandler()
              .handle(unit.get(Request.class), unit.get(Response.class));
        }, unit -> {
          unit.captured(Route.Complete.class).get(0)
              .handle((Request) null, (Response) null, (Optional) null);
        });
  }
}
