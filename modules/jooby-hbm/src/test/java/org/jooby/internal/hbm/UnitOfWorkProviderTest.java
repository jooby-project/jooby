package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertTrue;

import org.hibernate.Session;
import org.hibernate.SessionBuilder;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.engine.spi.SessionBuilderImplementor;
import org.jooby.hbm.UnitOfWork;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({UnitOfWorkProvider.class, ManagedSessionContext.class, RootUnitOfWork.class,
    ChildUnitOfWork.class, UnitOfWork.class })
public class UnitOfWorkProviderTest {

  @Test
  public void openSession() throws Exception {
    new MockUnit(SessionFactory.class, Session.class, SessionBuilderImplementor.class)
        .expect(hasBind(false))
        .expect(unit -> {
          SessionBuilder builder = unit.get(SessionBuilderImplementor.class);
          expect(unit.get(SessionFactory.class).withOptions()).andReturn(builder);
          expect(builder.openSession()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.constructor(RootUnitOfWork.class)
              .build(unit.get(Session.class));
        })
        .run(unit -> {
          UnitOfWork result = new UnitOfWorkProvider(unit.get(SessionFactory.class), b -> {}).get();
          assertTrue(result instanceof RootUnitOfWork);
        });
  }

  @Test
  public void currentSession() throws Exception {
    new MockUnit(SessionFactory.class, Session.class)
        .expect(hasBind(true))
        .expect(unit -> {
          expect(unit.get(SessionFactory.class).getCurrentSession())
              .andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.constructor(ChildUnitOfWork.class)
              .build(unit.get(Session.class));
        })
        .run(unit -> {
          UnitOfWork result = new UnitOfWorkProvider(unit.get(SessionFactory.class), b -> {}).get();
          assertTrue(result instanceof ChildUnitOfWork);
        });
  }

  @Test
  public void sessionBuilderConfigurer() throws Exception {
    new MockUnit(SessionFactory.class, Session.class, SessionBuilderImplementor.class)
        .expect(hasBind(false))
        .expect(unit -> {
          SessionBuilder builder = unit.get(SessionBuilderImplementor.class);
          expect(unit.get(SessionFactory.class).withOptions()).andReturn(builder);
          expect(builder.setQueryParameterValidation(true)).andReturn(builder);
          expect(builder.openSession()).andReturn(unit.get(Session.class));
        })
        .expect(unit -> {
          unit.constructor(RootUnitOfWork.class)
              .build(unit.get(Session.class));
        })
        .run(unit -> {
          UnitOfWork result = new UnitOfWorkProvider(unit.get(SessionFactory.class),
              b -> b.setQueryParameterValidation(true)).get();

          assertTrue(result instanceof RootUnitOfWork);
        });
  }

  private Block hasBind(final boolean b) {
    return unit -> {
      unit.mockStatic(ManagedSessionContext.class);

      expect(ManagedSessionContext.hasBind(unit.get(SessionFactory.class))).andReturn(b);
    };
  }
}
