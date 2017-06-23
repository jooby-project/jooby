package org.jooby.neo4j;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.DBType;
import iot.jcypher.database.remote.BoltDBAccess;
import javaslang.control.Try;
import org.jooby.Env;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.driver.v1.Session;
import org.neo4j.driver.v1.exceptions.ClientException;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.Properties;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.powermock.api.easymock.PowerMock.replayAll;

@RunWith(PowerMockRunner.class)
@PrepareForTest({BoltDBAccess.class, DBAccessFactory.class})
public class Neo4jTest {

  private Config $neo4j = ConfigFactory.parseResources(getClass(), "/neo4j/neo4j.conf");
  private BoltDBAccess remoteClient;

  @SuppressWarnings("unchecked")
  MockUnit.Block neo4j = unit -> {

    remoteClient = unit.mock(BoltDBAccess.class);
    unit.registerMock(BoltDBAccess.class, remoteClient);

    AnnotatedBindingBuilder<BoltDBAccess> rcABB = unit.mock(AnnotatedBindingBuilder.class);
    rcABB.toInstance(remoteClient);
    rcABB.toInstance(remoteClient);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(BoltDBAccess.class))).andReturn(rcABB);
    expect(binder.bind(Key.get(BoltDBAccess.class, Names.named("bolt://localhost:7687")))).andReturn(rcABB);

    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Try.CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j.db")).andReturn(false);
      })
      .expect(serviceKey(new Env.ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        PowerMock.mockStatic(DBAccessFactory.class);
        expect(DBAccessFactory.createDBAccess(isA(DBType.class), isA(Properties.class),
          isA(String.class), isA(String.class))).andReturn(remoteClient);
        replayAll();
        expect(remoteClient.getSession()).andReturn(unit.registerMock(Session.class));
      })
      .expect(unit -> {
        BoltDBAccess remoteClient = unit.get(BoltDBAccess.class);
        remoteClient.close();
      })
      .run(unit -> {
        Neo4j neo4j = new Neo4j();
        neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      }, unit -> {
        unit.captured(Try.CheckedRunnable.class).iterator().next().run();
      });
  }

  @Test(expected = ClientException.class)
  public void shouldFailWhenDbIsMissing() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j.db")).andReturn(false);
        expect(config.getString("server_root_uri")).andReturn("bolt://localhost:7687");
      })
      .expect(neo4j)
      .run(unit -> {
        Neo4j neo4j = new Neo4j();
        neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      });
  }

  @Test
  public void defaultsWithCustomAction() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j"))
          .andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j.db")).andReturn(true);
        expect(config.getConfig("neo4j.db")).andReturn(ConfigFactory.empty()
          .withValue("server_root_uri", ConfigValueFactory.fromAnyRef("bolt://localhost:7687")));
      })
      .expect(serviceKey(new Env.ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        PowerMock.mockStatic(DBAccessFactory.class);
        expect(DBAccessFactory.createDBAccess(isA(DBType.class), isA(Properties.class),
          isA(String.class), isA(String.class))).andReturn(remoteClient);
        replayAll();
        expect(remoteClient.getSession()).andReturn(unit.registerMock(Session.class));
      })
      .run(unit -> {
        new Neo4j()
          .properties((properties, config) -> {
            assertEquals("bolt://localhost:7687", properties.getProperty(DBProperties.SERVER_ROOT_URI));
          })
          .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      });
  }

  @Test
  public void defaultsConfig() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
      .expect(unit -> {
        assertEquals($neo4j, new Neo4j().config());
      });
  }

  @Test
  public void defaultsWithProperties() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j.db")).andReturn(false);
      })
      .expect(serviceKey(new Env.ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        PowerMock.mockStatic(DBAccessFactory.class);
        expect(DBAccessFactory.createDBAccess(isA(DBType.class), isA(Properties.class),
          isA(String.class), isA(String.class))).andReturn(remoteClient);
        replayAll();
        expect(remoteClient.getSession()).andReturn(unit.registerMock(Session.class));
      })
      .run(unit -> {
        new Neo4j()
          .properties((properties, config) -> {
            properties.put(DBProperties.SERVER_ROOT_URI, "bolt://localhost:7687");
          })
          .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      });
  }

  private MockUnit.Block serviceKey(final Env.ServiceKey serviceKey) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(serviceKey);
    };
  }
}