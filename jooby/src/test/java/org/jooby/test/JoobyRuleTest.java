package org.jooby.test;

import org.jooby.Jooby;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JoobyRule.class, Jooby.class })
public class JoobyRuleTest {

  @Test
  public void before() throws Exception {
    new MockUnit(Jooby.class)
        .expect(unit -> {
          Jooby app = unit.get(Jooby.class);
          app.start("server.join=false");
        })
        .run(unit -> {
          new JoobyRule(unit.get(Jooby.class)).before();
        });
  }

  @Test
  public void after() throws Exception {
    new MockUnit(Jooby.class)
        .expect(unit -> {
          Jooby app = unit.get(Jooby.class);
          app.stop();
        })
        .run(unit -> {
          new JoobyRule(unit.get(Jooby.class)).after();
        });
  }
}
