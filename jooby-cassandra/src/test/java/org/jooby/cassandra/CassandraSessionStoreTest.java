package org.jooby.cassandra;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.Map;

import org.jooby.Session.Builder;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.datastax.driver.core.BoundStatement;
import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.ResultSetFuture;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.google.common.collect.ImmutableMap;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CassandraSessionStore.class, Futures.class, BoundStatement.class })
public class CassandraSessionStoreTest {

  @SuppressWarnings("unchecked")
  private Block createTable = unit -> {
    ResultSetFuture rs = unit.mock(ResultSetFuture.class);

    Session session = unit.get(Session.class);
    expect(session.executeAsync(unit.capture(Statement.class))).andReturn(rs);

    unit.mockStatic(Futures.class);
    Futures.addCallback(eq(rs), unit.capture(FutureCallback.class));
  };

  private Block boundStatement = unit -> {
    BoundStatement statement = unit.constructor(BoundStatement.class)
        .args(PreparedStatement.class)
        .build(unit.get(PreparedStatement.class));

    unit.registerMock(BoundStatement.class, statement);
  };

  @Test
  public void newSessionStore30m() throws Exception {
    new MockUnit(Session.class)
        .expect(createTable)
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30m");
        });
  }

  @Test
  public void createTableStatement() throws Exception {
    new MockUnit(Session.class)
        .expect(createTable)
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30m");
        }, unit -> {
          String statement = unit.captured(Statement.class).iterator().next().toString();
          assertEquals("\n" +
              " CREATE TABLE IF NOT EXISTS session(\n" +
              "  id varchar,\n" +
              "  createdAt timestamp,\n" +
              "  accessedAt timestamp,\n" +
              "  savedAt timestamp,\n" +
              "  attributes map<varchar, varchar>,\n" +
              "  PRIMARY KEY(id))", statement.replace('\t', ' '));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void createTableSuccess() throws Exception {
    new MockUnit(Session.class, ResultSet.class)
        .expect(createTable)
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30m");
        }, unit -> {
          FutureCallback success = unit.captured(FutureCallback.class).iterator().next();
          success.onSuccess(unit.get(ResultSet.class));
        });
  }

  @SuppressWarnings("rawtypes")
  @Test
  public void createTableFailure() throws Exception {
    new MockUnit(Session.class, ResultSet.class)
        .expect(createTable)
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30m");
        }, unit -> {
          FutureCallback success = unit.captured(FutureCallback.class).iterator().next();
          success.onFailure(new IllegalStateException("intentional err"));
        });
  }

  @Test
  public void newSessionStore30s() throws Exception {
    new MockUnit(Session.class)
        .expect(createTable)
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30");
        });
  }

  @Test
  public void save() throws Exception {
    new MockUnit(Session.class, org.jooby.Session.class)
        .expect(createTable)
        .expect(session("sid", 1, 2, 3, ImmutableMap.of("foo", "bar")))
        .expect(insertInto(1800))
        .expect(boundStatement)
        .expect(unit -> {
          BoundStatement statement = unit.get(BoundStatement.class);

          expect(statement.bind("sid", new Date(1), new Date(2), new Date(3),
              ImmutableMap.of("foo", "bar"))).andReturn(statement);

          Session session = unit.get(Session.class);
          expect(session.execute(statement)).andReturn(null);
        })
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30m")
              .save(unit.get(org.jooby.Session.class));
        });
  }

  @Test
  public void create() throws Exception {
    new MockUnit(Session.class, org.jooby.Session.class)
        .expect(createTable)
        .expect(session("sid", 5, 6, 7, ImmutableMap.of("foo", "bar")))
        .expect(insertInto(30))
        .expect(boundStatement)
        .expect(unit -> {
          BoundStatement statement = unit.get(BoundStatement.class);

          expect(statement.bind("sid", new Date(5), new Date(6), new Date(7),
              ImmutableMap.of("foo", "bar"))).andReturn(statement);

          Session session = unit.get(Session.class);
          expect(session.execute(statement)).andReturn(null);
        })
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30")
              .save(unit.get(org.jooby.Session.class));
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(Session.class, org.jooby.Session.class, org.jooby.Session.Builder.class,
        ResultSet.class)
            .expect(createTable)
            .expect(unit -> {
              PreparedStatement statement = unit.mock(PreparedStatement.class);
              unit.registerMock(PreparedStatement.class, statement);

              Session session = unit.get(Session.class);

              expect(session.prepare("SELECT * FROM session WHERE id=?;")).andReturn(statement);
            })
            .expect(unit -> {
              Builder builder = unit.get(org.jooby.Session.Builder.class);
              expect(builder.sessionId()).andReturn("sid");
            })
            .expect(boundStatement)
            .expect(unit -> {
              BoundStatement statement = unit.get(BoundStatement.class);

              expect(statement.bind("sid")).andReturn(statement);

              Session session = unit.get(Session.class);
              expect(session.execute(statement)).andReturn(unit.get(ResultSet.class));
            })
            .expect(unit -> {
              Row row = unit.mock(Row.class);
              Date createdAt = new Date();
              Date accessedAt = new Date();
              Date savedAt = new Date();
              expect(row.getTimestamp("createdAt")).andReturn(createdAt);
              expect(row.getTimestamp("accessedAt")).andReturn(accessedAt);
              expect(row.getTimestamp("savedAt")).andReturn(savedAt);
              expect(row.getMap("attributes", String.class, String.class))
                  .andReturn(ImmutableMap.of("foo", "bar"));

              Builder builder = unit.get(org.jooby.Session.Builder.class);
              expect(builder.createdAt(createdAt.getTime())).andReturn(builder);
              expect(builder.accessedAt(accessedAt.getTime())).andReturn(builder);
              expect(builder.savedAt(savedAt.getTime())).andReturn(builder);
              expect(builder.set(ImmutableMap.of("foo", "bar"))).andReturn(builder);
              expect(builder.build()).andReturn(unit.get(org.jooby.Session.class));

              ResultSet rs = unit.get(ResultSet.class);
              expect(rs.one()).andReturn(row);
            })
            .expect(session("sid", 1, 2, 3, ImmutableMap.of("foo", "bar")))
            .expect(insertInto(30))
            .expect(boundStatement)
            .expect(unit -> {
              BoundStatement statement = unit.get(BoundStatement.class);

              expect(statement.bind("sid", new Date(1), new Date(2), new Date(3),
                  ImmutableMap.of("foo", "bar"))).andReturn(statement);

              Session session = unit.get(Session.class);
              expect(session.execute(statement)).andReturn(null);
            })
            .run(unit -> {
              new CassandraSessionStore(unit.get(Session.class), "30")
                  .get(unit.get(org.jooby.Session.Builder.class));
            });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(Session.class, org.jooby.Session.class)
        .expect(createTable)
        .expect(unit -> {
          PreparedStatement statement = unit.mock(PreparedStatement.class);
          unit.registerMock(PreparedStatement.class, statement);

          Session session = unit.get(Session.class);

          expect(session.prepare("DELETE FROM session WHERE id=?;")).andReturn(statement);
        })
        .expect(boundStatement)
        .expect(unit -> {
          BoundStatement statement = unit.get(BoundStatement.class);

          expect(statement.bind("sid")).andReturn(statement);

          Session session = unit.get(Session.class);
          expect(session.execute(statement)).andReturn(null);
        })
        .run(unit -> {
          new CassandraSessionStore(unit.get(Session.class), "30")
              .delete("sid");
        });
  }

  @Test
  public void getWithoutTtl() throws Exception {
    new MockUnit(Session.class, org.jooby.Session.class, org.jooby.Session.Builder.class,
        ResultSet.class)
            .expect(createTable)
            .expect(unit -> {
              PreparedStatement statement = unit.mock(PreparedStatement.class);
              unit.registerMock(PreparedStatement.class, statement);

              Session session = unit.get(Session.class);

              expect(session.prepare("SELECT * FROM session WHERE id=?;")).andReturn(statement);
            })
            .expect(unit -> {
              Builder builder = unit.get(org.jooby.Session.Builder.class);
              expect(builder.sessionId()).andReturn("sid");
            })
            .expect(boundStatement)
            .expect(unit -> {
              BoundStatement statement = unit.get(BoundStatement.class);

              expect(statement.bind("sid")).andReturn(statement);

              Session session = unit.get(Session.class);
              expect(session.execute(statement)).andReturn(unit.get(ResultSet.class));
            })
            .expect(unit -> {
              Row row = unit.mock(Row.class);
              Date createdAt = new Date();
              Date accessedAt = new Date();
              Date savedAt = new Date();
              expect(row.getTimestamp("createdAt")).andReturn(createdAt);
              expect(row.getTimestamp("accessedAt")).andReturn(accessedAt);
              expect(row.getTimestamp("savedAt")).andReturn(savedAt);
              expect(row.getMap("attributes", String.class, String.class))
                  .andReturn(ImmutableMap.of("foo", "bar"));

              Builder builder = unit.get(org.jooby.Session.Builder.class);
              expect(builder.createdAt(createdAt.getTime())).andReturn(builder);
              expect(builder.accessedAt(accessedAt.getTime())).andReturn(builder);
              expect(builder.savedAt(savedAt.getTime())).andReturn(builder);
              expect(builder.set(ImmutableMap.of("foo", "bar"))).andReturn(builder);
              expect(builder.build()).andReturn(unit.get(org.jooby.Session.class));

              ResultSet rs = unit.get(ResultSet.class);
              expect(rs.one()).andReturn(row);
            })
            .run(unit -> {
              new CassandraSessionStore(unit.get(Session.class), "0")
                  .get(unit.get(org.jooby.Session.Builder.class));
            });
  }

  private Block insertInto(final int ttl) {
    return unit -> {
      PreparedStatement statement = unit.mock(PreparedStatement.class);
      unit.registerMock(PreparedStatement.class, statement);

      Session session = unit.get(Session.class);
      expect(session.prepare(
          "INSERT INTO session (id,createdAt,accessedAt,savedAt,attributes) VALUES (?,?,?,?,?) USING TTL "
              + ttl + ";"))
                  .andReturn(statement);
    };
  }

  private Block session(final String sid, final long createdAt, final long accessedAt,
      final long savedAt, final Map<String, String> attributes) {
    return unit -> {
      org.jooby.Session session = unit.get(org.jooby.Session.class);
      expect(session.id()).andReturn(sid);
      expect(session.createdAt()).andReturn(createdAt);
      expect(session.accessedAt()).andReturn(accessedAt);
      expect(session.savedAt()).andReturn(savedAt);
      expect(session.attributes()).andReturn(attributes);
    };
  }

}
