package org.jooby.eventbus;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;
import static org.easymock.EasyMock.expect;
import org.greenrobot.eventbus.EventBus;
import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.funzy.Throwing;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EventBusby.class, EventBus.class})
public class EventBusbyTest {

  private MockUnit.Block defaultEventBus = unit -> {
    unit.mockStatic(EventBus.class);
    EventBus eventBus = unit.get(EventBus.class);
    expect(EventBus.getDefault()).andReturn(eventBus);
  };

  private MockUnit.Block bindEventBus = unit -> {
    EventBus eventBus = unit.get(EventBus.class);

    AnnotatedBindingBuilder<EventBus> abb = unit.mock(AnnotatedBindingBuilder.class);
    abb.toInstance(eventBus);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(EventBus.class)).andReturn(abb);
  };

  public static class MySubscriber {
  }

  @Test
  public void defaults() throws Exception {
    MySubscriber subscriber = new MySubscriber();
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class, EventBus.class)
        .expect(defaultEventBus)
        .expect(bindEventBus)
        .expect(lifeCycle(subscriber))
        .run(unit -> {
          new EventBusby()
              .register(subscriber)
              .register(MySubscriber.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).get(0).accept(unit.get(Registry.class));
          unit.captured(Throwing.Runnable.class).get(0).run();
        });
  }

  @Test
  public void supplyEventBus() throws Exception {
    MySubscriber subscriber = new MySubscriber();
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class, EventBus.class)
        .expect(bindEventBus)
        .expect(lifeCycle(subscriber))
        .run(unit -> {
          new EventBusby(() -> unit.get(EventBus.class))
              .register(subscriber)
              .register(MySubscriber.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).get(0).accept(unit.get(Registry.class));
          unit.captured(Throwing.Runnable.class).get(0).run();
        });
  }

  @Test
  public void provideEventBus() throws Exception {
    MySubscriber subscriber = new MySubscriber();
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class, EventBus.class)
        .expect(bindEventBus)
        .expect(lifeCycle(subscriber))
        .run(unit -> {
          new EventBusby(c -> unit.get(EventBus.class))
              .register(subscriber)
              .register(MySubscriber.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).get(0).accept(unit.get(Registry.class));
          unit.captured(Throwing.Runnable.class).get(0).run();
        });
  }

  private MockUnit.Block lifeCycle(MySubscriber subscriber) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);
      expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);

      EventBus eventBus = unit.get(EventBus.class);
      eventBus.register(subscriber);
      eventBus.register(subscriber);

      eventBus.unregister(subscriber);
      eventBus.unregister(subscriber);

      Registry registry = unit.get(Registry.class);
      expect(registry.require(MySubscriber.class)).andReturn(subscriber);
    };
  }
}
