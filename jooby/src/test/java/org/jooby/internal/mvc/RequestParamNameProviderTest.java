package org.jooby.internal.mvc;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

import org.jooby.Env;
import org.jooby.internal.RouteMetadata;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(RequestParam.class  )
public class RequestParamNameProviderTest {

  public void dummy(final String dummyparam) {

  }

  @Test
  public void asmname() throws Exception {
    Method m = RequestParamNameProviderTest.class.getDeclaredMethod("dummy", String.class);
    Parameter param = m.getParameters()[0];
    new MockUnit(Env.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          unit.mockStatic(RequestParam.class);
          expect(RequestParam.nameFor(param)).andReturn(null);
        })
        .run(unit -> {
          assertEquals("dummyparam",
              new RequestParamNameProviderImpl(new RouteMetadata(unit.get(Env.class))).name(param));
        });

  }
}
