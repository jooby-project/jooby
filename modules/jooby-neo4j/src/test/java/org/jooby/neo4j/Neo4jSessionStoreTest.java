package org.jooby.neo4j;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jooby.Session;
import org.jooby.Session.Builder;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

import iot.jcypher.database.IDBAccess;
import iot.jcypher.graph.GrNode;
import iot.jcypher.graph.GrProperty;
import iot.jcypher.query.JcQuery;
import iot.jcypher.query.JcQueryResult;
import iot.jcypher.query.api.IClause;
import iot.jcypher.query.api.modify.ModifyTerminal;
import iot.jcypher.query.api.modify.Set;
import iot.jcypher.query.api.pattern.Node;
import iot.jcypher.query.api.pattern.Property;
import iot.jcypher.query.api.returns.RSortable;
import iot.jcypher.query.factories.clause.CREATE;
import iot.jcypher.query.factories.clause.DO;
import iot.jcypher.query.factories.clause.MATCH;
import iot.jcypher.query.factories.clause.RETURN;
import iot.jcypher.query.values.JcNode;
import iot.jcypher.query.values.JcProperty;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Neo4jSessionStore.class, JcQuery.class, JcNode.class, MATCH.class, DO.class,
    System.class, RETURN.class, CREATE.class })
public class Neo4jSessionStoreTest {

  @Test
  public void newStore() throws Exception {
    new MockUnit(IDBAccess.class)
        .run(unit -> {
          new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m");
        });
  }

  @Test
  public void get() throws Exception {
    new MockUnit(IDBAccess.class, Session.Builder.class)
        .expect(sessionId("sidfoo"))
        .expect(newNode("n"))
        .expect(millis())
        .expect(GET("sidfoo", "session", TimeUnit.MINUTES.toMillis(30)))
        .expect(newQuery())
        .expect(execute())
        .expect(resultOf())
        .expect(newSession())
        .run(unit -> {
          Session session = new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m")
              .get(unit.get(Session.Builder.class));
          assertEquals(unit.get(Session.class), session);
        });
  }

  @Test
  public void getNull() throws Exception {
    new MockUnit(IDBAccess.class, Session.Builder.class)
        .expect(sessionId("sidfoo"))
        .expect(newNode("n"))
        .expect(millis())
        .expect(GET("sidfoo", "session", TimeUnit.SECONDS.toMillis(30)))
        .expect(newQuery())
        .expect(execute())
        .expect(emptyResultOf())
        .run(unit -> {
          Session session = new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30")
              .get(unit.get(Session.Builder.class));
          assertEquals(null, session);
        });
  }

  @Test
  public void save() throws Exception {
    new MockUnit(IDBAccess.class, Session.class)
        .expect(sid("sidfoo", ImmutableMap.of("foo", "bar"), 1L, 2L, 3L))
        .expect(newNode("n"))
        .expect(millis())
        .expect(SAVE("sidfoo", "session", 1L, 2L, 3L, TimeUnit.MINUTES.toMillis(30)))
        .expect(newQuery())
        .expect(execute())
        .expect(resultOf())
        .expect(unit -> {
          GrProperty foo = unit.mock(GrProperty.class);
          expect(foo.getName()).andReturn("foo");
          GrNode node = unit.get(GrNode.class);
          expect(node.getProperties()).andReturn(ImmutableList.of(foo));
        })
        .run(unit -> {
          new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m")
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void saveDeleteUnsetProps() throws Exception {
    new MockUnit(IDBAccess.class, Session.class)
        .expect(sid("sidfoo", ImmutableMap.of("foo", "bar"), 1L, 2L, 3L))
        .expect(newNode("n"))
        .expect(millis())
        .expect(SAVE("sidfoo", "session", 1L, 2L, 3L, TimeUnit.MINUTES.toMillis(30)))
        .expect(newQuery())
        .expect(execute())
        .expect(resultOf())
        .expect(unit -> {
          GrProperty bar = unit.mock(GrProperty.class);
          expect(bar.getName()).andReturn("bar");
          GrNode node = unit.get(GrNode.class);
          expect(node.getProperties()).andReturn(ImmutableList.of(bar));
        })
        .expect(newQuery())
        .expect(REMOVE("sidfoo", "session", "bar"))
        .expect(execute())
        .run(unit -> {
          new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m")
              .save(unit.get(Session.class));
        });
  }

  @Test
  public void create() throws Exception {
    new MockUnit(IDBAccess.class, Session.class)
        .expect(sid("sidfoo", ImmutableMap.of("foo", "bar"), 1L, 2L, 3L))
        .expect(newNode("n"))
        .expect(millis())
        .expect(SAVE("sidfoo", "session", 1L, 2L, 3L, TimeUnit.MINUTES.toMillis(30)))
        .expect(newQuery())
        .expect(execute())
        .expect(emptyResultOf())
        .expect(newQuery())
        .expect(CREATE("sidfoo", "session", 1L, 2L, 3L, TimeUnit.MINUTES.toMillis(30)))
        .expect(execute())
        .expect(emptyResultOf())
        .run(unit -> {
          new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m")
              .create(unit.get(Session.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void delete() throws Exception {
    new MockUnit(IDBAccess.class)
        .expect(newNode("n"))
        .expect(unit -> {
          {
            unit.mockStatic(MATCH.class);

            Node node = unit.mock(Node.class);

            Property<Node> prop = unit.mock(Property.class);
            expect(prop.value("sidfoo")).andReturn(node);

            expect(node.label("session")).andReturn(node);
            expect(node.property("_id")).andReturn(prop);
            expect(MATCH.node(unit.get(JcNode.class))).andReturn(node);
          }

          {
            unit.mockStatic(DO.class);
            JcNode node = unit.get(JcNode.class);
            expect(DO.DELETE(node)).andReturn(null);
          }
        })
        .expect(newQuery())
        .expect(execute())
        .run(unit -> {
          new Neo4jSessionStore(unit.get(IDBAccess.class), "session", "30m")
              .delete("sidfoo");
        });
  }

  @SuppressWarnings("unchecked")
  private Block CREATE(final String sid, final String label, final long accessedAt,
      final long createdAt, final long savedAt, final long millis) {
    return unit -> {
      {
        unit.mockStatic(CREATE.class);

        Node node = unit.mock(Node.class);

        Property<Node> id = unit.mock(Property.class);
        expect(id.value(sid)).andReturn(node);

        Property<Node> _accessedAt = unit.mock(Property.class);
        expect(_accessedAt.value(accessedAt)).andReturn(node);

        Property<Node> _savedAt = unit.mock(Property.class);
        expect(_savedAt.value(savedAt)).andReturn(node);

        Property<Node> _createdAt = unit.mock(Property.class);
        expect(_createdAt.value(createdAt)).andReturn(node);

        Property<Node> _expire = unit.mock(Property.class);
        expect(_expire.value(millis)).andReturn(node);

        Property<Node> _foo = unit.mock(Property.class);
        expect(_foo.value("bar")).andReturn(node);

        expect(node.label(label)).andReturn(node);
        expect(node.property("_id")).andReturn(id);
        expect(node.property("_accessedAt")).andReturn(_accessedAt);
        expect(node.property("_savedAt")).andReturn(_savedAt);
        expect(node.property("_createdAt")).andReturn(_createdAt);
        expect(node.property("_expire")).andReturn(_expire);
        expect(node.property("foo")).andReturn(_foo);
        expect(CREATE.node(unit.get(JcNode.class))).andReturn(node);
      }
    };
  }

  @SuppressWarnings("unchecked")
  private Block SAVE(final String sid, final String label, final long accessedAt,
      final long createdAt, final long savedAt, final long timeout) {
    return unit -> {
      {
        unit.mockStatic(MATCH.class);

        Node node = unit.mock(Node.class);

        Property<Node> prop = unit.mock(Property.class);
        expect(prop.value(sid)).andReturn(node);

        expect(node.label(label)).andReturn(node);
        expect(node.property("_id")).andReturn(prop);
        expect(MATCH.node(unit.get(JcNode.class))).andReturn(node);
      }

      DO_SET(unit, "_accessedAt", accessedAt);
      DO_SET(unit, "_createdAt", createdAt);
      DO_SET(unit, "_savedAt", savedAt);
      DO_SET(unit, "_expire", timeout);
      DO_SET(unit, "foo", "bar");
    };
  }

  @SuppressWarnings("unchecked")
  private void DO_SET(final MockUnit unit, final String name, final Object value) {
    unit.mockStatic(DO.class);

    JcProperty prop = unit.mock(JcProperty.class);
    JcNode node = unit.get(JcNode.class);
    expect(node.property(name)).andReturn(prop);

    Set<ModifyTerminal> mod = unit.mock(Set.class);
    expect(mod.to(value)).andReturn(null);

    expect(DO.SET(prop)).andReturn(mod);
  }

  @SuppressWarnings("unchecked")
  private Block GET(final String sid, final String label, final long timeout) {
    return unit -> {
      {
        unit.mockStatic(MATCH.class);

        Node node = unit.mock(Node.class);

        Property<Node> prop = unit.mock(Property.class);
        expect(prop.value(sid)).andReturn(node);

        expect(node.label(label)).andReturn(node);
        expect(node.property("_id")).andReturn(prop);
        expect(MATCH.node(unit.get(JcNode.class))).andReturn(node);
      }

      {
        unit.mockStatic(DO.class);

        JcNode node = unit.get(JcNode.class);

        Set<ModifyTerminal> setprop = unit.mock(Set.class);
        expect(setprop.to(timeout))
            .andReturn(unit.registerMock(ModifyTerminal.class));

        JcProperty prop = unit.mock(JcProperty.class);
        expect(node.property("_expire")).andReturn(prop);
        expect(DO.SET(prop)).andReturn(setprop);
      }

      {
        unit.mockStatic(RETURN.class);
        expect(RETURN.value(unit.get(JcNode.class)))
            .andReturn(unit.registerMock(RSortable.class));
      }
    };
  }

  @SuppressWarnings("unchecked")
  private Block REMOVE(final String sid, final String label, final String property) {
    return unit -> {
      {
        unit.mockStatic(MATCH.class);

        Node node = unit.mock(Node.class);

        Property<Node> prop = unit.mock(Property.class);
        expect(prop.value(sid)).andReturn(node);

        expect(node.label(label)).andReturn(node);
        expect(node.property("_id")).andReturn(prop);
        expect(MATCH.node(unit.get(JcNode.class))).andReturn(node);
      }

      {
        unit.mockStatic(DO.class);

        JcProperty prop = unit.mock(JcProperty.class);

        JcNode node = unit.get(JcNode.class);
        expect(node.property(property)).andReturn(prop);

        expect(DO.REMOVE(prop)).andReturn(unit.mock(ModifyTerminal.class));
      }

      {
        unit.mockStatic(RETURN.class);
        expect(RETURN.value(unit.get(JcNode.class)))
            .andReturn(unit.registerMock(RSortable.class));
      }
    };
  }

  private Block newSession() {
    return unit -> {
      Builder builder = unit.get(Session.Builder.class);

      GrProperty accessedAt = unit.mock(GrProperty.class);
      expect(accessedAt.getValue()).andReturn(1L);
      expect(builder.accessedAt(1L)).andReturn(builder);

      GrProperty createdAt = unit.mock(GrProperty.class);
      expect(createdAt.getValue()).andReturn(2L);
      expect(builder.createdAt(2L)).andReturn(builder);

      GrProperty savedAt = unit.mock(GrProperty.class);
      expect(savedAt.getValue()).andReturn(3L);
      expect(builder.savedAt(3L)).andReturn(builder);

      GrProperty foo = unit.mock(GrProperty.class);
      expect(foo.getName()).andReturn("foo").times(2);
      expect(foo.getValue()).andReturn("bar");
      expect(builder.set("foo", "bar")).andReturn(builder);

      GrProperty sid = unit.mock(GrProperty.class);
      expect(sid.getName()).andReturn("_id");

      GrNode node = unit.get(GrNode.class);
      expect(node.getProperty("_accessedAt")).andReturn(accessedAt);
      expect(node.getProperty("_createdAt")).andReturn(createdAt);
      expect(node.getProperty("_savedAt")).andReturn(savedAt);

      expect(node.getProperties()).andReturn(ImmutableList.of(foo, sid));

      expect(builder.build()).andReturn(unit.registerMock(Session.class));
    };
  }

  private Block resultOf() {
    return unit -> {
      GrNode node = unit.registerMock(GrNode.class);
      JcQueryResult result = unit.get(JcQueryResult.class);
      expect(result.resultOf(unit.get(JcNode.class))).andReturn(ImmutableList.of(node));
    };
  }

  private Block emptyResultOf() {
    return unit -> {
      JcQueryResult result = unit.get(JcQueryResult.class);
      expect(result.resultOf(unit.get(JcNode.class))).andReturn(ImmutableList.of());
    };
  }

  private Block millis() {
    return unit -> {
      unit.mockStatic(System.class);
      expect(System.currentTimeMillis()).andReturn(0L);
    };
  }

  private Block newNode(final String name) {
    return unit -> {
      JcNode node = unit.constructor(JcNode.class)
          .build(name);
      unit.registerMock(JcNode.class, node);
    };
  }

  private Block execute() {
    return unit -> {
      JcQueryResult result = unit.registerMock(JcQueryResult.class);
      IDBAccess db = unit.get(IDBAccess.class);
      expect(db.execute(unit.get(JcQuery.class))).andReturn(result);
    };
  }

  private Block newQuery() {
    return unit -> {
      JcQuery query = unit.constructor(JcQuery.class)
          .build();
      query.setClauses(unit.capture(IClause[].class));
      unit.registerMock(JcQuery.class, query);
    };
  }

  private Block sessionId(final String sid) {
    return unit -> {
      Builder builder = unit.get(Session.Builder.class);
      expect(builder.sessionId()).andReturn(sid);
    };
  }

  private Block sid(final String sid, final Map<String, String> attrs, final long accessedAt,
      final long createdAt, final long savedAt) {
    return unit -> {
      Session session = unit.get(Session.class);
      expect(session.id()).andReturn(sid);
      expect(session.attributes()).andReturn(attrs);

      expect(session.accessedAt()).andReturn(accessedAt);
      expect(session.createdAt()).andReturn(createdAt);
      expect(session.savedAt()).andReturn(savedAt);
    };
  }
}
