package org.jooby.jooq;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertEquals;

import java.sql.Connection;
import java.sql.SQLException;

import javax.inject.Provider;
import javax.sql.DataSource;

import org.jooby.test.MockUnit;
import org.jooq.exception.DataAccessException;
import org.junit.Test;

public class DSConnectionProviderTest {

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Provider.class)
        .run(unit -> {
          new DSConnectionProvider(unit.get(Provider.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void acquire() throws Exception {
    new MockUnit(Provider.class, Connection.class, DataSource.class)
        .expect(unit -> {
          Connection conn = unit.get(Connection.class);

          DataSource ds = unit.get(DataSource.class);
          expect(ds.getConnection()).andReturn(conn);

          Provider<DataSource> provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(ds);
        })
        .run(unit -> {
          assertEquals(unit.get(Connection.class),
              new DSConnectionProvider(unit.get(Provider.class)).acquire());
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = DataAccessException.class)
  public void acquireFailure() throws Exception {
    new MockUnit(Provider.class, DataSource.class)
        .expect(unit -> {
          DataSource ds = unit.get(DataSource.class);
          expect(ds.getConnection()).andThrow(new SQLException("intentional err"));

          Provider<DataSource> provider = unit.get(Provider.class);
          expect(provider.get()).andReturn(ds);
        })
        .run(unit -> {
          new DSConnectionProvider(unit.get(Provider.class)).acquire();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void release() throws Exception {
    new MockUnit(Provider.class, Connection.class)
        .expect(unit -> {
          Connection conn = unit.get(Connection.class);
          conn.close();
        })
        .run(unit -> {
          new DSConnectionProvider(unit.get(Provider.class)).release(unit.get(Connection.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = DataAccessException.class)
  public void releaseFailure() throws Exception {
    new MockUnit(Provider.class, Connection.class)
        .expect(unit -> {
          Connection conn = unit.get(Connection.class);
          conn.close();
          expectLastCall().andThrow(new SQLException("intentional err"));
        })
        .run(unit -> {
          new DSConnectionProvider(unit.get(Provider.class)).release(unit.get(Connection.class));
        });
  }

}
