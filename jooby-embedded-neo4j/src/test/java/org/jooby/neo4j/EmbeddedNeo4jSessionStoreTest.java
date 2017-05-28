package org.jooby.neo4j;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.easymock.EasyMock.anyObject;
import static org.junit.Assert.assertEquals;

import com.google.common.collect.ImmutableMap;
import com.graphaware.neo4j.expire.ExpirationModule;
import com.graphaware.runtime.GraphAwareRuntime;
import org.apache.commons.lang3.tuple.Pair;
import org.jooby.Session;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Label;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.Transaction;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({EmbeddedNeo4jSessionStore.class, LinkedHashMap.class, ExpirationModule.class})
public class EmbeddedNeo4jSessionStoreTest {
  private static GraphDatabaseService dbService;
  private static GraphAwareRuntime graphAwareRuntime;
  private static Session.Builder sb;

  @SuppressWarnings("unchecked")
  MockUnit.Block boot = unit -> {
    dbService = unit.get(GraphDatabaseService.class);
    graphAwareRuntime = unit.get(GraphAwareRuntime.class);
    ExpirationModule expirationModule = unit.get(ExpirationModule.class);
    expect(graphAwareRuntime.getModule(isA(ExpirationModule.class.getClass()))).andReturn(expirationModule);
    graphAwareRuntime.start();
    expectLastCall();
    graphAwareRuntime.waitUntilStarted();
    expectLastCall();
  };

  long now = System.currentTimeMillis();

  Map<String, String> attrs = ImmutableMap.of("k.v", "v", "$d", "d");
  Map<String, Object> properties;

  MockUnit.Block saveSession = unit -> {

    Session session = unit.get(Session.class);
    expect(session.id()).andReturn("1234");
    expect(session.accessedAt()).andReturn(now);
    expect(session.createdAt()).andReturn(now);
    expect(session.savedAt()).andReturn(now);
    expect(session.attributes()).andReturn(attrs);

    Node node = unit.mock(Node.class);
    node.setProperty(isA(String.class), isA(Object.class));
    expectLastCall().anyTimes();
    expect(node.hasProperty(isA(String.class))).andReturn(false);
    expect(dbService.findNode(isA(Label.class), isA(String.class), isA(Object.class))).andReturn(node);
  };

  MockUnit.Block updateSession = unit -> {

    Session session = unit.get(Session.class);
    expect(session.id()).andReturn("1234");
    expect(session.accessedAt()).andReturn(now);
    expect(session.createdAt()).andReturn(now);
    expect(session.savedAt()).andReturn(now);
    expect(session.attributes()).andReturn(attrs);

    Node node = unit.mock(Node.class);
    node.setProperty(isA(String.class), isA(Object.class));
    expectLastCall().anyTimes();
    expect(dbService.findNode(isA(Label.class), isA(String.class), isA(Object.class))).andReturn(null);
    expect(dbService.createNode(isA(Label.class))).andReturn(node);
  };

  @SuppressWarnings("unchecked")
  MockUnit.Block getFromSession = unit -> {
    long now = System.currentTimeMillis();

    Node node = unit.mock(Node.class);
    expect(node.getAllProperties()).andReturn(properties);

    Map sessionMap = unit.constructor(LinkedHashMap.class)
      .args(Map.class).build(properties);

    expect(sessionMap.remove("_accessedAt")).andReturn(now);
    expect(sessionMap.remove("_createdAt")).andReturn(now);
    expect(sessionMap.remove("_savedAt")).andReturn(now);
    expect(sessionMap.remove("_id")).andReturn(now);
    expect(sessionMap.remove("_expire")).andReturn(null);
    sessionMap.forEach(unit.capture(BiConsumer.class));

    sb = unit.mock(Session.Builder.class);
    expect(sb.sessionId()).andReturn("1234");
    expect(sb.accessedAt(now)).andReturn(sb);
    expect(sb.createdAt(now)).andReturn(sb);
    expect(sb.savedAt(now)).andReturn(sb);

    Map.Entry<String, Object> property = properties.entrySet().iterator().next();
    expect(sb.set(property.getKey(), property.getValue().toString())).andReturn(sb);

    expect(sb.build()).andReturn(unit.get(Session.class));
    expect(dbService.findNode(isA(Label.class), isA(String.class), isA(Object.class))).andReturn(node);
  };

  MockUnit.Block handleTransaction = unit -> {
    Transaction t = unit.mock(Transaction.class);
    t.success();
    expectLastCall();
    t.close();
    expectLastCall();
    expect(dbService.beginTx()).andReturn(t);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .run(unit -> {
        new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L);
      });
  }

  @Test(expected = NullPointerException.class)
  public void defaultsNullDB() throws Exception {
    new EmbeddedNeo4jSessionStore(null, null, "sess", 60L);
  }

  @Test
  public void create() throws Exception {
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(saveSession)
      .expect(handleTransaction)
      .run(unit -> {
        new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L)
          .create(unit.get(Session.class));
      });
  }

  @Test
  public void save() throws Exception {
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(saveSession)
      .expect(handleTransaction)
      .run(unit -> {
        new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L)
          .save(unit.get(Session.class));
      });
  }

  @Test
  public void update() throws Exception {
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(updateSession)
      .expect(handleTransaction)
      .run(unit -> {
        new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L)
          .save(unit.get(Session.class));
      });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void get() throws Exception {
    properties = Collections.singletonMap("a.b", "c");
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(getFromSession)
      .expect(handleTransaction)
      .run(unit -> {
        EmbeddedNeo4jSessionStore nss = new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L);
        assertEquals(unit.get(Session.class), nss.get(sb));
      }, unit -> {
        BiConsumer<String, String> setter = unit.captured(BiConsumer.class).get(0);
        setter.accept("a\uFF0Eb", "c");
      });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void getDollar() throws Exception {
    properties = Collections.singletonMap("$ab", "c");
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(getFromSession)
      .expect(handleTransaction)
      .run(unit -> {
        EmbeddedNeo4jSessionStore nss = new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L);
        assertEquals(unit.get(Session.class), nss.get(sb));
      }, unit -> {
        BiConsumer<String, String> setter = unit.captured(BiConsumer.class).get(0);
        setter.accept("\uFF04ab", "c");
      });
  }

  @Test
  public void getExpired() throws Exception {
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(unit -> {
        sb = unit.mock(Session.Builder.class);
        expect(sb.sessionId()).andReturn("1234");
        expect(dbService.findNode(isA(Label.class), isA(String.class), isA(Object.class))).andReturn(null);
      })
      .expect(unit -> {
        Transaction t = unit.mock(Transaction.class);
        t.close();
        expectLastCall();
        expect(dbService.beginTx()).andReturn(t);
      })
      .run(unit -> {
        EmbeddedNeo4jSessionStore nss = new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L);
        assertEquals(null, nss.get(sb));
      });
  }

  @Test
  public void delete() throws Exception {
    new MockUnit(Session.class, GraphDatabaseService.class, GraphAwareRuntime.class, ExpirationModule.class)
      .expect(boot)
      .expect(unit -> {
        Node node = unit.mock(Node.class);
        node.delete();
        expectLastCall();
        expect(dbService.findNode(isA(Label.class), isA(String.class), isA(Object.class))).andReturn(node);
      })
      .expect(handleTransaction)
      .run(unit -> {
        new EmbeddedNeo4jSessionStore(dbService, graphAwareRuntime, "sess", 60L).delete("1234");
      });
  }
}