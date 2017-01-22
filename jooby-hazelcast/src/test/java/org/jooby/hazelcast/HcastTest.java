package org.jooby.hazelcast;

import static org.easymock.EasyMock.expect;

import java.util.Properties;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.typesafe.config.Config;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hcast.class, Hazelcast.class, com.hazelcast.config.Config.class, Hazelcast.class })
public class HcastTest {

  @SuppressWarnings("unchecked")
  MockUnit.Block bindings = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.getConfig("hazelcast")).andReturn(new Hcast().config().getConfig("hazelcast"));

    Properties properties = new Properties();
    properties.setProperty("hazelcast.logging.type", "slf4j");

    com.hazelcast.config.Config config = unit
        .mockConstructor(com.hazelcast.config.Config.class);
    unit.registerMock(com.hazelcast.config.Config.class, config);
    expect(config.setProperties(properties)).andReturn(config);

    AnnotatedBindingBuilder<com.hazelcast.config.Config> abbConfig = unit
        .mock(AnnotatedBindingBuilder.class);
    abbConfig.toInstance(config);

    HazelcastInstance hcast = unit.mock(HazelcastInstance.class);
    unit.mockStatic(Hazelcast.class);
    hcast.shutdown();

    expect(Hazelcast.newHazelcastInstance(config)).andReturn(hcast);
    AnnotatedBindingBuilder<HazelcastInstance> abbHI = unit
        .mock(AnnotatedBindingBuilder.class);
    abbHI.toInstance(hcast);
    unit.registerMock(HazelcastInstance.class, hcast);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(com.hazelcast.config.Config.class)).andReturn(abbConfig);

    expect(binder.bind(HazelcastInstance.class)).andReturn(abbHI);
  };

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(bindings)
        .expect(onStop)
        .run(unit -> {
          new Hcast()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withConfigurer() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Consumer.class)
        .expect(bindings)
        .expect(unit -> {
          Consumer<com.hazelcast.config.Config> consumer = unit.get(Consumer.class);
          consumer.accept(unit.get(com.hazelcast.config.Config.class));
        })
        .expect(onStop)
        .run(unit -> {
          new Hcast()
              .doWith(unit.get(Consumer.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

}
