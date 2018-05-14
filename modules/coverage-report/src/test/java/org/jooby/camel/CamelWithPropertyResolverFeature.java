package org.jooby.camel;

import static org.junit.Assert.assertEquals;

import org.apache.camel.CamelContext;
import org.apache.camel.ProducerTemplate;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class CamelWithPropertyResolverFeature extends ServerFeature {

  {
    use(ConfigFactory.empty()
        .withValue("you", ConfigValueFactory.fromAnyRef("Camel")));

    use(new Camel()
        .routes((router, config) -> {
          router.from("direct:props").transform().simple("Hi ${you}!").process(exchange -> {
            String message = exchange.getIn().getBody(String.class);
            assertEquals("Hi Camel!", message);
          });
        }));

    get("/", req -> req.require(CamelContext.class).getClass().getName());

    get("/props", req -> {
      req.require(ProducerTemplate.class).sendBody("direct:props", "...");
      return req.path();
    });
  }

  @Test
  public void props() throws Exception {
    request()
        .get("/props")
        .expect("/props");
  }
}
