package org.jooby.internal.ebean;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.Connection;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.jooby.test.MockUnit;
import org.junit.Test;

public class ForwardingDataSourceTest {

  @Test
  public void connection() throws Exception {
    new MockUnit(DataSource.class, Connection.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).getConnection()).andReturn(unit.get(Connection.class));
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(unit.get(Connection.class), ds.getConnection());
        });
  }

  @Test
  public void connectionWithUserAndPassword() throws Exception {
    new MockUnit(DataSource.class, Connection.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).getConnection("u", "p"))
              .andReturn(unit.get(Connection.class));
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(unit.get(Connection.class), ds.getConnection("u", "p"));
        });
  }

  @Test
  public void loginTimeout() throws Exception {
    new MockUnit(DataSource.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).getLoginTimeout()).andReturn(100);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(100, ds.getLoginTimeout());
        });
  }

  @Test
  public void logWriter() throws Exception {
    PrintWriter writer = new PrintWriter(new StringWriter());

    new MockUnit(DataSource.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).getLogWriter()).andReturn(writer);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(writer, ds.getLogWriter());
        });
  }

  @Test
  public void parentLogger() throws Exception {
    new MockUnit(DataSource.class, Logger.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).getParentLogger()).andReturn(unit.get(Logger.class));
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(unit.get(Logger.class), ds.getParentLogger());
        });
  }

  @Test
  public void isWrappedFor() throws Exception {
    new MockUnit(DataSource.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).isWrapperFor(Integer.class)).andReturn(false);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(false, ds.isWrapperFor(Integer.class));
        });
  }

  @Test
  public void setLoginTimeout() throws Exception {
    new MockUnit(DataSource.class)
        .expect(unit -> {
          unit.get(DataSource.class).setLoginTimeout(100);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          ds.setLoginTimeout(100);
        });
  }

  @Test
  public void setLogWriter() throws Exception {
    PrintWriter writer = new PrintWriter(new StringWriter());
    new MockUnit(DataSource.class)
        .expect(unit -> {
          unit.get(DataSource.class).setLogWriter(writer);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          ds.setLogWriter(writer);
        });
  }

  @Test
  public void unwrap() throws Exception {
    new MockUnit(DataSource.class)
        .expect(unit -> {
          expect(unit.get(DataSource.class).unwrap(Integer.class)).andReturn(1);
        })
        .run(unit -> {
          ForwardingDataSource ds = new ForwardingDataSource(() -> unit.get(DataSource.class));
          assertEquals(1, (int) ds.unwrap(Integer.class));
        });
  }

}
