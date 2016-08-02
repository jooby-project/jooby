package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.context.internal.ManagedSessionContext;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SessionProvider.class, ManagedSessionContext.class })
public class SessionProviderTest {

  @Test
  public void openSession() throws Exception {
    new MockUnit(SessionFactory.class, Session.class)
        .expect(hasBind(false))
        .expect(unit -> {
          expect(unit.get(SessionFactory.class).openSession()).andReturn(unit.get(Session.class));
        })
        .run(unit -> {
          Session result = new SessionProvider(unit.get(SessionFactory.class)).get();
          assertEquals(unit.get(Session.class), result);
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
        .run(unit -> {
          Session result = new SessionProvider(unit.get(SessionFactory.class)).get();
          assertEquals(unit.get(Session.class), result);
        });
  }

  private Block hasBind(final boolean b) {
    return unit -> {
      unit.mockStatic(ManagedSessionContext.class);

      expect(ManagedSessionContext.hasBind(unit.get(SessionFactory.class))).andReturn(b);
    };
  }
}
