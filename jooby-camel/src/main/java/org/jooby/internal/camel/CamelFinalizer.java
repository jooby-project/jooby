package org.jooby.internal.camel;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.RoutesBuilder;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jooby.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CamelFinalizer implements Managed {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private DefaultCamelContext ctx;

  private ProducerTemplate producer;

  private ConsumerTemplate consumer;

  @Inject
  public CamelFinalizer(final GuiceInjector injector,
      final DefaultCamelContext ctx,
      final @Named("camel.routes") Set<Object> routes,
      final RouteBuilder rb,
      final ProducerTemplate producer,
      final ConsumerTemplate consumer) throws Exception {
    this.ctx = ctx;
    this.producer = producer;
    this.consumer = consumer;

    this.ctx.setInjector(injector);

    for (Object route : routes) {
      if (route instanceof RoutesBuilder) {
        this.ctx.addRoutes((RoutesBuilder) route);
      }
    }
    this.ctx.addRoutes(rb);
  }

  @Override
  public void start() throws Exception {
    this.ctx.start();
    this.producer.start();
    this.consumer.start();
  }

  @Override
  public void stop() throws Exception {
    try {
      this.consumer.stop();
    } catch (Exception ex) {
      log.error("Can't stop consumer template", ex);
    }
    try {
      this.producer.stop();
    } catch (Exception ex) {
      log.error("Can't stop producer template", ex);
    }
    try {
      this.ctx.stop();
    } catch (Exception ex) {
      log.error("Can't stop camel context template", ex);
    }

  }

}
