package org.jooby.micrometer;

import io.micrometer.prometheus.PrometheusMeterRegistry;
import static org.easymock.EasyMock.expect;
import org.jooby.MediaType;
import org.jooby.Request;
import org.jooby.Response;
import org.jooby.Result;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class PrometheusHandlerTest {

  @Test
  public void handle() throws Exception {
    new MockUnit(PrometheusMeterRegistry.class, Request.class, Response.class)
        .expect(unit -> {
          PrometheusMeterRegistry registry = unit.get(PrometheusMeterRegistry.class);
          expect(registry.scrape()).andReturn("scrape");

          Request req = unit.get(Request.class);
          expect(req.require(PrometheusMeterRegistry.class)).andReturn(registry);

          Response rsp = unit.get(Response.class);
          expect(rsp.type(MediaType.plain)).andReturn(rsp);
          rsp.send(unit.capture(Result.class));
        })
        .run(unit -> {
          new PrometheusHandler()
              .handle(unit.get(Request.class), unit.get(Response.class));
        }, unit->{
          assertEquals("scrape", unit.captured(Result.class).get(0).get());
        });
  }
}
