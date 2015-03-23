package org.jooby.internal.camel;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.jooby.MockUnit;
import org.junit.Test;

import com.google.inject.Injector;

public class GuiceInjectorTest {

  @Test
  public void defaults() throws Exception {
    new MockUnit(Injector.class)
        .run(unit -> {
          new GuiceInjector(unit.get(Injector.class));
        });
  }

  @Test
  public void newInstance() throws Exception {
    Object value = new Object();
    new MockUnit(Injector.class)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Object.class)).andReturn(value);
        })
        .run(unit -> {
          assertEquals(value,
              new GuiceInjector(unit.get(Injector.class)).newInstance(Object.class));
        });
  }

  @Test
  public void newSingletonInstance() throws Exception {
    Object value = new Object();
    new MockUnit(Injector.class)
        .expect(unit -> {
          Injector injector = unit.get(Injector.class);
          expect(injector.getInstance(Object.class)).andReturn(value);
        })
        .run(unit -> {
          assertEquals(value, new GuiceInjector(unit.get(Injector.class)).newInstance(
              Object.class, new Object()));
        });
  }

  @Test(expected = NullPointerException.class)
  public void nullInjector() throws Exception {
    new GuiceInjector(null);
  }
}
