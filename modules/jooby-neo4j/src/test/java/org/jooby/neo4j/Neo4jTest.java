package org.jooby.neo4j;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.driver.v1.AuthToken;
import org.neo4j.driver.v1.AuthTokens;
import org.neo4j.driver.v1.Driver;
import org.neo4j.driver.v1.GraphDatabase;
import org.neo4j.driver.v1.Session;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.neo4j.logging.slf4j.Slf4jLogProvider;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.graphaware.neo4j.expire.ExpirationModuleBootstrapper;
import com.graphaware.runtime.RuntimeRegistry;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigException;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import iot.jcypher.database.DBAccessFactory;
import iot.jcypher.database.DBProperties;
import iot.jcypher.database.IDBAccess;
import iot.jcypher.database.embedded.EmbeddedDBAccess;
import iot.jcypher.database.remote.BoltDBAccess;
import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Neo4j.class, BoltDBAccess.class, DBAccessFactory.class, Properties.class,
    GraphDatabaseFactory.class, RuntimeRegistry.class, EmbeddedDBAccess.class, GraphDatabase.class,
    AuthTokens.class })
public class Neo4jTest {

  @Test(expected = ConfigException.Missing.class)
  public void missingDatabase() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", false))
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() throws Exception {
    assertEquals(ConfigFactory.empty(getClass().getName().toLowerCase() + ".conf")
        .withValue("neo4j.session.label", ConfigValueFactory.fromAnyRef("session")),
        new Neo4j().config());
  }

  @Test
  public void rm() throws Exception {
    Path dir = Paths.get("target", "foo");
    Neo4j.rm(dir);
    Files.createDirectories(dir);
    Files.createFile(dir.resolve("foo.txt"));
    Path subdir = dir.resolve("bar");
    Files.createDirectories(subdir);
    Files.createFile(subdir.resolve("bar.txt"));

    assertEquals(true, Files.exists(dir));
    Neo4j.rm(dir);
    assertEquals(false, Files.exists(dir));
    Neo4j.rm(dir);
    assertEquals(false, Files.exists(dir));
  }

  @Test
  public void memDirect() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jmem").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("mem.url", false))
        .expect(hasPath("mem", false))
        .expect(hasPath("mem", false))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("neo4j.mem", false))
        .expect(confString("application.tmpdir", dir.toString()))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "mem"))
        .expect(embeddedAccess("mem"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j("mem");
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block bind(final Class class1, final int i) {
    return unit -> {
      List<Consumer> consumers = unit.captured(Consumer.class);
      consumers.get(i).accept(Key.get(class1));
    };
  }

  @Test
  public void memAsProperty() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jmem").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", true))
        .expect(confString("db", "mem"))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("db", true))
        .expect(notAConf("db"))
        .expect(hasPath("neo4j.db", false))
        .expect(confString("application.tmpdir", dir.toString()))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "db"))
        .expect(embeddedAccess("db"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @Test
  public void bolt() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", true))
        .expect(confString("db.url", "bolt://localhost:7687"))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("db", true))
        .expect(confConf("db", ConfigFactory.empty()))
        .expect(hasPath("neo4j.db", false))
        .expect(serviceKey())
        .expect(confString("db.user", "test"))
        .expect(confString("db.password", "test"))
        .expect(bolt("bolt://localhost:7687", "db", "test", "test"))
        .expect(props())
        .expect(setProp("server_root_uri", "bolt://localhost:7687"))
        .expect(boltAccess("db"))
        .expect(logdb("bolt://localhost:7687", null))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(Driver.class, 0),
            bind(Session.class, 1));
  }

  @Test
  public void customConnection() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("mydb.url", true))
        .expect(confString("mydb.url", "bolt://localhost:7687"))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("mydb", true))
        .expect(confConf("mydb", ConfigFactory.empty()))
        .expect(hasPath("neo4j.mydb", false))
        .expect(serviceKey())
        .expect(confString("mydb.user", "test"))
        .expect(confString("mydb.password", "test"))
        .expect(bolt("bolt://localhost:7687", "mydb", "test", "test"))
        .expect(props())
        .expect(setProp("server_root_uri", "bolt://localhost:7687"))
        .expect(boltAccess("mydb"))
        .expect(logdb("bolt://localhost:7687", null))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j("mydb");
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(Driver.class, 0),
            bind(Session.class, 1));
  }

  @Test
  public void fs() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jfs").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", true))
        .expect(confString("db", "fs"))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("db", true))
        .expect(notAConf("db"))
        .expect(hasPath("neo4j.db", false))
        .expect(confString("application.tmpdir", dir.toString()))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "db"))
        .expect(embeddedAccess("db"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @Test
  public void fspath() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("dbdir").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath(dbdir + ".url", false))
        .expect(hasPath(dbdir.toString(), false))
        .expect(hasPath(dbdir.toString(), false))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("neo4j." + dbdir.toString(), false))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, dbdir.toString()))
        .expect(embeddedAccess(dbdir.toString()))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j(dbdir);
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @Test
  public void oneModule() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jfs").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", true))
        .expect(confString("db", dbdir.toString()))
        .expect(hasPath("com.graphaware", true))
        .expect(confConf("com.graphaware",
            ConfigFactory.empty().withValue("module",
                ConfigValueFactory.fromAnyRef(
                    (ImmutableMap.of("class", ExpirationModuleBootstrapper.class.getName(),
                        "nodeExpirationProperty", "_expire"))))))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("db", false))
        .expect(hasPath("neo4j.db", false))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(putProp("com.graphaware.module.m1.1", ExpirationModuleBootstrapper.class.getName()))
        .expect(putProp("com.graphaware.module.m1.nodeExpirationProperty", "_expire"))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "db"))
        .expect(embeddedAccess("db"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @Test
  public void multipleModule() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jfs").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", true))
        .expect(confString("db", dbdir.toString()))
        .expect(hasPath("com.graphaware", true))
        .expect(confConf("com.graphaware",
            ConfigFactory.empty().withValue("module",
                ConfigValueFactory.fromAnyRef(Arrays.asList(
                    (ImmutableMap.of("class", ExpirationModuleBootstrapper.class.getName(),
                        "nodeExpirationProperty", "_expire")),
                    (ImmutableMap.of("class", "com.foo.Foo",
                        "foo", "bar")))))))
        .expect(hasPath("neo4j", false))
        .expect(hasPath("db", false))
        .expect(hasPath("neo4j.db", false))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(putProp("com.graphaware.module.m1.1", ExpirationModuleBootstrapper.class.getName()))
        .expect(putProp("com.graphaware.module.m1.nodeExpirationProperty", "_expire"))
        .expect(putProp("com.graphaware.module.m2.2", "com.foo.Foo"))
        .expect(putProp("com.graphaware.module.m2.foo", "bar"))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "db"))
        .expect(embeddedAccess("db"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            });
  }

  @SuppressWarnings({"unchecked", "deprecation" })
  @Test
  public void embedded() throws Exception {
    Path dir = Paths.get("target", "des").toAbsolutePath();
    Path dbdir = dir.resolve("neo4jfs").toAbsolutePath();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(hasPath("db.url", false))
        .expect(hasPath("db", true))
        .expect(confString("db", dbdir.toString()))
        .expect(hasPath("com.graphaware", false))
        .expect(hasPath("neo4j", true))
        .expect(confConf("neo4j",
            ConfigFactory.empty().withValue("unsupported.dbms.block_size.array_properties",
                ConfigValueFactory.fromAnyRef(120))))
        .expect(hasPath("db", false))
        .expect(hasPath("neo4j.db", false))
        .expect(props())
        .expect(setProp("database_dir", dbdir.toString()))
        .expect(putProp("unsupported.dbms.block_size.array_properties", 120))
        .expect(serviceKey())
        .expect(dbFactory(dbdir, "db"))
        .expect(embeddedAccess("db"))
        .expect(logdb(null, dbdir.toString()))
        .expect(onStop())
        .expect(unit -> {
          GraphDatabaseBuilder builder = unit.get(GraphDatabaseBuilder.class);
          expect(builder.setConfig("unsupported.dbms.block_size.array_properties", "120"))
              .andReturn(builder);
        })
        .run(unit -> {
          Neo4j neo4j = new Neo4j();
          neo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop(),
            bind(GraphDatabaseService.class, 0),
            bind(IDBAccess.class, 1),
            unit -> {
              IDBAccess db = unit.get(IDBAccess.class);
              bind(db.getClass(), 2).run(unit);
            }, unit -> {
              unit.captured(BiConsumer.class).get(0)
                  .accept("unsupported.dbms.block_size.array_properties", "120");
            });
  }

  private Block closeOnStop() {
    return unit -> {
      unit.captured(CheckedRunnable.class).get(0).run();
    };
  }

  private Block onStop() {
    return unit -> {
      Env env = unit.get(Env.class);
      unit.get(IDBAccess.class).close();
      expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
    };
  }

  private Block logdb(final String uri, final String dir) {
    return unit -> {
      Properties props = unit.get(Properties.class);
      expect(props.getProperty(DBProperties.SERVER_ROOT_URI)).andReturn(uri);
      expect(props.getProperty(DBProperties.DATABASE_DIR)).andReturn(dir);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block embeddedAccess(final String dbkey) {
    return unit -> {
      EmbeddedDBAccess db = unit.constructor(EmbeddedDBAccess.class)
          .build();
      unit.registerMock(IDBAccess.class, db);
      db.initialize(unit.get(Properties.class));

      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(db);
      lbb.toInstance(db);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(IDBAccess.class))).andReturn(lbb);
      expect(binder.bind(Key.get(db.getClass()))).andReturn(lbb);

      ServiceKey keys = unit.get(ServiceKey.class);
      keys.generate(eq(IDBAccess.class), eq(dbkey), unit.capture(Consumer.class));
      keys.generate(eq(db.getClass()), eq(dbkey), unit.capture(Consumer.class));
    };
  }

  @SuppressWarnings("unchecked")
  private Block boltAccess(final String dbkey) {
    return unit -> {
      BoltDBAccess db = unit.constructor(BoltDBAccess.class)
          .build();
      unit.registerMock(IDBAccess.class, db);
      db.initialize(unit.get(Properties.class));

      ServiceKey keys = unit.get(ServiceKey.class);
      keys.generate(eq(IDBAccess.class), eq(dbkey), unit.capture(Consumer.class));
      keys.generate(eq(db.getClass()), eq(dbkey), unit.capture(Consumer.class));
    };
  }

  @SuppressWarnings("unchecked")
  private Block dbFactory(final Path dbdir, final String dbkey) {
    return unit -> {
      GraphDatabaseService dbservice = unit.registerMock(GraphDatabaseService.class);
      // unit.mockStatic(RuntimeRegistry.class);
      // expect(RuntimeRegistry.getStartedRuntime(dbservice)).andReturn(null);

      LinkedBindingBuilder<GraphDatabaseService> lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(dbservice);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(GraphDatabaseService.class))).andReturn(lbb);

      ServiceKey keys = unit.get(ServiceKey.class);
      keys.generate(eq(GraphDatabaseService.class), eq(dbkey), unit.capture(Consumer.class));

      GraphDatabaseBuilder dbbuilder = unit.registerMock(GraphDatabaseBuilder.class);
      expect(dbbuilder.newGraphDatabase()).andReturn(dbservice);

      GraphDatabaseFactory factory = unit.constructor(GraphDatabaseFactory.class)
          .build();
      expect(factory.setUserLogProvider(isA(Slf4jLogProvider.class))).andReturn(factory);
      expect(factory.newEmbeddedDatabaseBuilder(dbdir.toFile())).andReturn(dbbuilder);
    };
  }

  private Block setProp(final String key, final String value) {
    return unit -> {
      Properties props = unit.get(Properties.class);
      expect(props.setProperty(key, value)).andReturn(null);
    };
  }

  private Block putProp(final String key, final Object value) {
    return unit -> {
      Properties props = unit.get(Properties.class);
      expect(props.put(key, value)).andReturn(null);
    };
  }

  private Block serviceKey() {
    return unit -> {
      ServiceKey keys = unit.registerMock(Env.ServiceKey.class);
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(keys);
    };
  }

  @SuppressWarnings("unchecked")
  private Block props() {
    return unit -> {
      Properties props = unit.constructor(Properties.class)
          .build();
      props.forEach(unit.capture(BiConsumer.class));
      expectLastCall().times(0, 1);
      unit.registerMock(Properties.class, props);
    };
  }

  private Block notAConf(final String path) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getConfig(path))
          .andThrow(new ConfigException.WrongType(ConfigFactory.empty().origin(), path));
    };
  }

  private Block confString(final String path, final String value) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getString(path)).andReturn(value);
    };
  }

  private Block hasPath(final String path, final boolean value) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.hasPath(path)).andReturn(value);
    };
  }

  private Block confConf(final String path, final Config value) {
    return unit -> {
      Config conf = unit.get(Config.class);
      expect(conf.getConfig(path)).andReturn(value);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block bolt(final String uri, final String dbkey, final String user, final String pass) {
    return unit -> {
      AuthToken token = unit.mock(AuthToken.class);

      unit.mockStatic(AuthTokens.class);
      expect(AuthTokens.basic(user, pass)).andReturn(token);

      Binder binder = unit.get(Binder.class);

      Session session = unit.registerMock(Session.class);

      LinkedBindingBuilder<Session> lbbs = unit.mock(LinkedBindingBuilder.class);
      lbbs.toInstance(session);
      expect(binder.bind(Key.get(Session.class))).andReturn(lbbs);

      Driver driver = unit.registerMock(Driver.class);
      expect(driver.session()).andReturn(session);

      LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
      lbb.toInstance(driver);

      expect(binder.bind(Key.get(Driver.class))).andReturn(lbb);

      unit.mockStatic(GraphDatabase.class);
      expect(GraphDatabase.driver(uri, token)).andReturn(driver);

      ServiceKey keys = unit.get(ServiceKey.class);
      keys.generate(eq(Driver.class), eq(dbkey), unit.capture(Consumer.class));
      keys.generate(eq(Session.class), eq(dbkey), unit.capture(Consumer.class));
    };
  }
}
