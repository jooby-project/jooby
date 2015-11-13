package org.jooby.camel;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CamelCustomConfigFeature extends ServerFeature {

  {
    use(new Camel()
        .doWith((ctx, config) -> {
          ctx.addRoutes(new RouteBuilder() {

            @Override
            public void configure() throws Exception {
              from("direct:noop").to("mock:out");
            }
          });
        }));

    get("/noop", req -> {
      req.require(ProducerTemplate.class).sendBody("direct:noop", "NOOP");
      return req.path();
    });
  }

  @Test
  public void noop() throws Exception {
    request()
        .get("/noop")
        .expect("/noop");
  }

}
