package org.jooby.jooq;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.test.MockUnit;
import org.jooq.Configuration;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({DSLCtxProvider.class, DSL.class })
public class DSLCtxProviderTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Configuration.class)
        .run(unit -> {
          new DSLCtxProvider(unit.get(Configuration.class));
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Configuration.class, DSLContext.class)
        .expect(unit -> {
          unit.mockStatic(DSL.class);
          expect(DSL.using(unit.get(Configuration.class))).andReturn(unit.get(DSLContext.class));
        })
        .run(unit -> {
          assertEquals(unit.get(DSLContext.class),
              new DSLCtxProvider(unit.get(Configuration.class)).get());
        });
  }

}
