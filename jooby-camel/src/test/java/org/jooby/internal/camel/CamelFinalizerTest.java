package org.jooby.internal.camel;

import static org.easymock.EasyMock.expectLastCall;

import java.util.Set;

import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.impl.DefaultCamelContext;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.common.collect.Sets;

public class CamelFinalizerTest {

  RouteBuilder rb = new RouteBuilder() {

    @Override
    public void configure() throws Exception {
    }
  };

  MockUnit.Block ctx = unit -> {

    DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
    ctx.setInjector(unit.get(GuiceInjector.class));

    ctx.addRoutes(rb);
    ctx.addRoutes(unit.get(RouteBuilder.class));
  };

  @Test
  public void defaults() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class));
        });
  }

  @Test
  public void start() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .expect(unit -> {
          DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
          ctx.start();

          ProducerTemplate producer = unit.get(ProducerTemplate.class);
          producer.start();

          ConsumerTemplate consumer = unit.get(ConsumerTemplate.class);
          consumer.start();
        })
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class)).start();;
        });
  }

  @Test
  public void safeStop() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .expect(unit -> {
          DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
          ctx.stop();

          ProducerTemplate producer = unit.get(ProducerTemplate.class);
          producer.stop();

          ConsumerTemplate consumer = unit.get(ConsumerTemplate.class);
          consumer.stop();
        })
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class)).stop();;
        });
  }

  @Test
  public void ctxNoStop() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .expect(unit -> {
          DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
          ctx.stop();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));

          ProducerTemplate producer = unit.get(ProducerTemplate.class);
          producer.stop();

          ConsumerTemplate consumer = unit.get(ConsumerTemplate.class);
          consumer.stop();
        })
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class)).stop();;
        });
  }

  @Test
  public void producerNoStop() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .expect(unit -> {
          DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
          ctx.stop();

          ProducerTemplate producer = unit.get(ProducerTemplate.class);
          producer.stop();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));

          ConsumerTemplate consumer = unit.get(ConsumerTemplate.class);
          consumer.stop();
        })
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class)).stop();;
        });
  }

  @Test
  public void consumerNoStop() throws Exception {
    Set<Object> routes = Sets.newHashSet(rb, new Object());
    new MockUnit(GuiceInjector.class, DefaultCamelContext.class, RouteBuilder.class,
        ProducerTemplate.class, ConsumerTemplate.class)
        .expect(ctx)
        .expect(unit -> {
          DefaultCamelContext ctx = unit.get(DefaultCamelContext.class);
          ctx.stop();

          ProducerTemplate producer = unit.get(ProducerTemplate.class);
          producer.stop();

          ConsumerTemplate consumer = unit.get(ConsumerTemplate.class);
          consumer.stop();
          expectLastCall().andThrow(new IllegalStateException("intentional err"));
        })
        .run(unit -> {
          new CamelFinalizer(unit.get(GuiceInjector.class),
              unit.get(DefaultCamelContext.class), routes,
              unit.get(RouteBuilder.class), unit.get(ProducerTemplate.class), unit
                  .get(ConsumerTemplate.class)).stop();;
        });
  }

}
