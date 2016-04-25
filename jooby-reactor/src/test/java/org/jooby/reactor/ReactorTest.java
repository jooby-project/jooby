package org.jooby.reactor;

import static org.junit.Assert.assertEquals;

import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

public class ReactorTest {

  @Test
  public void configure() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .run(unit -> {
          new Reactor()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() throws Exception {
    assertEquals(ConfigFactory.parseResources(Reactor.class, "reactor.conf"),
        new Reactor().config());
  }

}
