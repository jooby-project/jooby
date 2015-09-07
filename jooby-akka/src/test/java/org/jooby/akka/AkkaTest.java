package org.jooby.akka;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import akka.actor.ActorSystem;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Akka.class, ActorSystem.class })
public class AkkaTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, ActorSystem.class)
        .expect(unit -> {
          unit.mockStatic(ActorSystem.class);
          expect(ActorSystem.create("default", unit.get(Config.class)))
              .andReturn(unit.get(ActorSystem.class));
        })
        .expect(unit -> {
          ActorSystem sys = unit.get(ActorSystem.class);

          LinkedBindingBuilder<ActorSystem> lbbSys = unit.mock(LinkedBindingBuilder.class);
          lbbSys.toInstance(sys);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(ActorSystem.class))).andReturn(lbbSys);
        })
        .run(unit -> {
          new Akka().configure(unit.get(Env.class), unit.get(Config.class),
              unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void named() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, ActorSystem.class)
        .expect(unit -> {
          unit.mockStatic(ActorSystem.class);
          expect(ActorSystem.create("default", unit.get(Config.class)))
              .andReturn(unit.get(ActorSystem.class));
        })
        .expect(unit -> {

          ActorSystem sys = unit.get(ActorSystem.class);

          LinkedBindingBuilder<ActorSystem> lbbSys = unit.mock(LinkedBindingBuilder.class);
          lbbSys.toInstance(sys);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(ActorSystem.class, Names.named("default"))))
              .andReturn(lbbSys);
        })
        .run(unit -> {
          new Akka().named().configure(unit.get(Env.class), unit.get(Config.class),
              unit.get(Binder.class));
        });
  }

  @Test
  public void config() {
    Config config = new Akka().config();
    assertEquals(akka.event.slf4j.Slf4jLogger.class.getName(),
        config.getStringList("akka.loggers").get(0));
    assertEquals("INFO", config.getString("akka.loglevel"));
  }
}
