package org.jooby.camel;

import static java.util.Objects.requireNonNull;
import static org.junit.Assert.assertEquals;

import javax.inject.Inject;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.jooby.test.ServerFeature;
import org.junit.Test;

import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

public class CamelWithInjectableRoutesFeature extends ServerFeature {

  public static class Railway extends RouteBuilder {

    private Printer printer;

    @Inject
    public Railway(final Printer printer) {
      this.printer = requireNonNull(printer, "A printer is required.");
    }

    @Override
    public void configure() throws Exception {
      from("direct:di").transform().simple("Hi ${you}!").process(printer);
    }
  }

  public static class Printer implements Processor {

    @Override
    public void process(final Exchange exchange) throws Exception {
      String message = exchange.getIn().getBody(String.class);
      assertEquals("Hi Camel!", message);
    }

  }

  {
    use(ConfigFactory.empty()
        .withValue("you", ConfigValueFactory.fromAnyRef("Camel")));

    use(new Camel().routes(Railway.class));

    get("/di", req -> {
      req.require(ProducerTemplate.class).sendBody("direct:di", "...");
      return req.path();
    });
  }

  @Test
  public void di() throws Exception {
    request()
        .get("/di")
        .expect("/di");
  }
}
