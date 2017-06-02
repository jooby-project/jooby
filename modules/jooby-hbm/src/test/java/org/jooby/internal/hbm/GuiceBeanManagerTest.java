package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.InjectionTarget;

import org.jooby.Registry;
import org.jooby.test.MockUnit;
import org.junit.Test;

import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;

public class GuiceBeanManagerTest {

  public static class Listener {
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void beanManager() throws Exception {
    new GuiceBeanManager();
    Listener value = new Listener();
    new MockUnit(Promise.class, Registry.class)
        .expect(unit -> {
          Registry registry = unit.get(Registry.class);
          expect(registry.require(Listener.class)).andReturn(value);
          Future future = unit.mock(Future.class);
          expect(future.get()).andReturn(registry);
          expect(unit.get(Promise.class).future()).andReturn(future);
        })
        .run(unit -> {
          BeanManager bm = GuiceBeanManager.beanManager(unit.get(Promise.class));
          AnnotatedType<Listener> type = bm.createAnnotatedType(Listener.class);
          assertNotNull(type);
          InjectionTarget<Listener> injectionTarget = bm.createInjectionTarget(type);
          CreationalContext<Listener> ctx = bm.createCreationalContext(null);
          assertEquals(value, injectionTarget.produce(ctx));
          injectionTarget.inject(value, ctx);
          injectionTarget.postConstruct(value);

          injectionTarget.preDestroy(value);
          injectionTarget.dispose(value);
          ctx.release();
        });
  }

  @Test(expected = UnsupportedOperationException.class)
  public void bmUOE() throws Exception {
    BeanManager bm = GuiceBeanManager.beanManager(null);
    bm.getELResolver();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void atUOE() throws Exception {
    BeanManager bm = GuiceBeanManager.beanManager(null);
    AnnotatedType<Listener> type = bm.createAnnotatedType(Listener.class);
    type.getBaseType();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void itUOE() throws Exception {
    BeanManager bm = GuiceBeanManager.beanManager(null);
    AnnotatedType<Listener> type = bm.createAnnotatedType(Listener.class);
    InjectionTarget<Listener> injectionTarget = bm.createInjectionTarget(type);
    injectionTarget.getInjectionPoints();
  }

  @Test(expected = UnsupportedOperationException.class)
  public void ccUOE() throws Exception {
    BeanManager bm = GuiceBeanManager.beanManager(null);
    CreationalContext<Object> cc = bm.createCreationalContext(null);
    cc.push(null);
  }
}
