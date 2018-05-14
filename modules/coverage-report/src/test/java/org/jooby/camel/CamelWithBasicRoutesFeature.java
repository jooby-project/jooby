package org.jooby.camel;

import org.apache.camel.ProducerTemplate;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CamelWithBasicRoutesFeature extends ServerFeature {

  {
    use(new Camel()
        .routes((router, config) -> {
          router.from("direct:noop").to("mock:out");
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
