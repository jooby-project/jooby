package org.jooby.ebean;

import static org.easymock.EasyMock.expect;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Ebeanby.class, System.class })
public class Issue589 {

  @Test
  public void shouldTurnOffEbeanShutdownHook() throws Exception {
    new MockUnit()
        .expect(unit -> {
          unit.mockStatic(System.class);
          expect(System.setProperty("ebean.registerShutdownHook", "false")).andReturn(null);
        })
        .run(unit -> {
          new Ebeanby();
        });
  }

}
