package org.jooby.internal;

import static org.easymock.EasyMock.expect;

import java.lang.reflect.Method;

import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({PreDestroyImpl.class, Method.class })
public class PreDestroyImplTest {

  @Test
  public void stop() throws Exception {
    Object source = new Object();
    new MockUnit(Method.class)
        .expect(unit -> {
          Method method = unit.get(Method.class);
          expect(method.invoke(source)).andReturn(null);
        })
        .run(unit -> {
          new PreDestroyImpl(source, unit.get(Method.class)).stop();
        });
  }

  @Test
  public void startNoop() throws Exception {
    Object source = new Object();
    new MockUnit(Method.class)
        .run(unit -> {
          new PreDestroyImpl(source, unit.get(Method.class)).start();
        });
  }

}
