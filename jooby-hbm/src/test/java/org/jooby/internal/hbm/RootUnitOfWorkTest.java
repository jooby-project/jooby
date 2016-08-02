package org.jooby.internal.hbm;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;

import org.hibernate.FlushMode;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.context.internal.ManagedSessionContext;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SessionImplementor;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({RootUnitOfWork.class, ManagedSessionContext.class })
public class RootUnitOfWorkTest {

  private Block bind = unit -> {
    unit.mockStatic(ManagedSessionContext.class);
    expect(ManagedSessionContext.bind(unit.get(SessionImplementor.class))).andReturn(null);
  };

  private Block begin = unit -> {
    Transaction trx = unit.get(Transaction.class);
    trx.begin();
  };

  private Block flush = unit -> {
    SessionImplementor session = unit.get(SessionImplementor.class);
    session.flush();
  };

  private Block commit = unit -> {
    Transaction trx = unit.get(Transaction.class);
    trx.commit();
  };

  private Block unbind = unit -> {
    SessionFactoryImplementor sf = unit.mock(SessionFactoryImplementor.class);

    unit.mockStatic(ManagedSessionContext.class);
    expect(ManagedSessionContext.unbind(sf)).andReturn(null);

    SessionImplementor session = unit.get(SessionImplementor.class);
    expect(session.getSessionFactory()).andReturn(sf);
  };

  private Block rollback = unit -> {
    Transaction trx = unit.get(Transaction.class);
    trx.rollback();
  };

  @Test
  public void newUnitOfWork() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class));
        });
  }

  @Test
  public void commitTrx() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(flush)
        .expect(trx(true))
        .expect(commit)
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .commit();
        });
  }

  @Test
  public void committedTrx() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(flush)
        .expect(trx(false))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .commit();
        });
  }

  @Test
  public void commitShouldIgnoreFlushOnReadOnlyTrx() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(setReadOnly(true))
        .expect(trx(false))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setReadOnly()
              .commit();
        });
  }

  @Test
  public void setReadonly() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(setReadOnly(true))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setReadOnly();
        });
  }

  @Test
  public void setReadonlyFailure() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(setReadOnly(true, true))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setReadOnly();
        });
  }

  @Test
  public void ignoreReadOnlyOnRollbackOnly() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setRollbackOnly()
              .setReadOnly();
        });
  }

  @Test
  public void rollback() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(true))
        .expect(rollback)
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .rollback();
        });
  }

  @Test
  public void rollbackInactiveTransaction() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .rollback();
        });
  }

  @Test
  public void apply() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .expect(flush)
        .expect(trx(true))
        .expect(commit)
        .expect(close(false))
        .expect(unbind)
        .run(unit -> {
          Session result = new RootUnitOfWork(unit.get(SessionImplementor.class))
              .apply(session -> {
                return session;
              });
          assertEquals(unit.get(SessionImplementor.class), result);
        });
  }

  @Test
  public void applyRevertReadOnly() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(setReadOnly(true))
        .expect(begin)
        .expect(trx(true))
        .expect(commit)
        .expect(setReadOnly(false))
        .expect(close(false))
        .expect(unbind)
        .run(unit -> {
          Session result = new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setReadOnly()
              .apply(session -> {
                return session;
              });
          assertEquals(unit.get(SessionImplementor.class), result);
        });
  }

  @Test(expected = IllegalStateException.class)
  public void applyShouldCloseSessionAndUnbind() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .expect(flush)
        .expect(trx(true))
        .expect(commit)
        .expect(unit -> {
          expectLastCall().andThrow(new IllegalStateException("intentional error"));
        })
        .expect(close(false))
        .expect(unbind)
        .run(unit -> {
          Session result = new RootUnitOfWork(unit.get(SessionImplementor.class))
              .apply(session -> {
                return session;
              });
          assertEquals(unit.get(SessionImplementor.class), result);
        });
  }

  @Test
  public void accept() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .expect(flush)
        .expect(trx(true))
        .expect(commit)
        .expect(close(false))
        .expect(unbind)
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .accept(session -> {
                assertEquals(unit.get(SessionImplementor.class), session);
              });
        });
  }

  @Test
  public void applySilentCloseError() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .expect(flush)
        .expect(trx(true))
        .expect(commit)
        .expect(close(true))
        .expect(unbind)
        .run(unit -> {
          Session result = new RootUnitOfWork(unit.get(SessionImplementor.class))
              .apply(session -> {
                return session;
              });
          assertEquals(unit.get(SessionImplementor.class), result);
        });
  }

  @Test(expected = NullPointerException.class)
  public void applyRollback() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .expect(flush)
        .expect(trx(true))
        .expect(rollback)
        .expect(close(false))
        .expect(unbind)
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .apply(session -> {
                throw new NullPointerException();
              });
        });
  }

  private Block close(final boolean b) {
    return unit -> {
      SessionImplementor session = unit.get(SessionImplementor.class);
      session.close();
      if (b) {
        expectLastCall().andThrow(new IllegalStateException("intentional error"));
      }
    };
  }

  private Block setReadOnly(final boolean b) {
    return setReadOnly(b, false);
  }

  private Block setReadOnly(final boolean b, final boolean ex) {
    return unit -> {
      Connection conn = unit.mock(Connection.class);
      conn.setReadOnly(b);
      if (ex) {
        expectLastCall().andThrow(new IllegalStateException("intentional err"));
      }
      SessionImplementor session = unit.get(SessionImplementor.class);
      expect(session.connection()).andReturn(conn);
      if (b) {
        session.setDefaultReadOnly(b);
        session.setHibernateFlushMode(FlushMode.MANUAL);
      }
    };
  }

  @Test
  public void commitIgnoreWhenRollbackOnly() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setRollbackOnly()
              .commit();
        });
  }

  @Test
  public void beginTrx() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(false))
        .expect(begin)
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .begin();
        });
  }

  @Test
  public void joinTrx() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .expect(trx(true))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .begin();
        });
  }

  @Test
  public void beginIgnoredOnRollbackOnly() throws Exception {
    new MockUnit(SessionImplementor.class)
        .expect(bind)
        .expect(flushMode(FlushMode.AUTO))
        .run(unit -> {
          new RootUnitOfWork(unit.get(SessionImplementor.class))
              .setRollbackOnly()
              .begin();
        });
  }

  private Block trx(final boolean active) {
    return unit -> {
      Transaction trx = unit.mock(Transaction.class);
      expect(trx.isActive()).andReturn(active);
      unit.registerMock(Transaction.class, trx);

      SessionImplementor session = unit.get(SessionImplementor.class);
      expect(session.getTransaction()).andReturn(trx);
    };
  }

  private Block flushMode(final FlushMode flushMode) {
    return unit -> {
      SessionImplementor session = unit.get(SessionImplementor.class);
      session.setHibernateFlushMode(flushMode);
    };
  }
}
