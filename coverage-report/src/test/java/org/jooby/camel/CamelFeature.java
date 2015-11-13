package org.jooby.camel;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.jooby.test.ServerFeature;
import org.junit.Test;

public class CamelFeature extends ServerFeature {

  {
    use(new Camel());

    get("/", req -> req.require(CamelContext.class).getClass().getName());

    get("/producer", req -> req.require(ProducerTemplate.class).getClass().getName());

    get("/consumer", req -> req.require(ConsumerTemplate.class).getClass().getName());
  }

  @Test
  public void boot() throws Exception {
    request()
        .get("/")
        .expect("org.apache.camel.impl.DefaultCamelContext");

    request()
        .get("/producer")
        .expect("org.apache.camel.impl.DefaultProducerTemplate");

    request()
        .get("/consumer")
        .expect("org.apache.camel.impl.DefaultConsumerTemplate");
  }

}
