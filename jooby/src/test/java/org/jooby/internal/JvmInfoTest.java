package org.jooby.internal;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.management.ManagementFactory;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JvmInfo.class, ManagementFactory.class })
public class JvmInfoTest {

  @Test
  public void emptyConstructor() {
    new JvmInfo();
  }

  @Test
  public void pid() {
    assertTrue(JvmInfo.pid() > 0);
  }

  @Test
  public void piderr() throws Exception {
    new MockUnit()
    .expect(unit -> {
      unit.mockStatic(ManagementFactory.class);
      expect(ManagementFactory.getRuntimeMXBean()).andThrow(new RuntimeException());
    })
    .run(unit -> {
      assertEquals(-1, JvmInfo.pid());
    });
  }

}
