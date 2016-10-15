package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.codecs.configuration.CodecRegistry;
import org.jooby.Env;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.rx.Rx;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.mongodb.ConnectionString;
import com.mongodb.ReadConcern;
import com.mongodb.ReadPreference;
import com.mongodb.WriteConcern;
import com.mongodb.async.client.MongoClientSettings;
import com.mongodb.connection.ClusterSettings;
import com.mongodb.connection.ClusterType;
import com.mongodb.connection.ConnectionPoolSettings;
import com.mongodb.connection.ServerSettings;
import com.mongodb.connection.SocketSettings;
import com.mongodb.connection.SslSettings;
import com.mongodb.rx.client.AggregateObservable;
import com.mongodb.rx.client.DistinctObservable;
import com.mongodb.rx.client.FindObservable;
import com.mongodb.rx.client.ListCollectionsObservable;
import com.mongodb.rx.client.ListDatabasesObservable;
import com.mongodb.rx.client.MapReduceObservable;
import com.mongodb.rx.client.MongoClient;
import com.mongodb.rx.client.MongoClients;
import com.mongodb.rx.client.MongoCollection;
import com.mongodb.rx.client.MongoDatabase;
import com.mongodb.rx.client.MongoObservable;
import com.mongodb.rx.client.ObservableAdapter;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import javaslang.control.Try;
import javaslang.control.Try.CheckedRunnable;
import rx.Observable;
import rx.Scheduler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MongoRx.class, MongoClients.class, MongoClientSettings.class,
    ClusterSettings.class, ConnectionPoolSettings.class, SocketSettings.class, ServerSettings.class,
    SslSettings.class, Rx.class, Observable.class })
public class MongodbRxTest {

  private Block settings = unit -> {
    MongoClientSettings.Builder builder = unit.powerMock(MongoClientSettings.Builder.class);
    expect(builder.clusterSettings(unit.get(ClusterSettings.class))).andReturn(builder);
    expect(builder.connectionPoolSettings(unit.get(ConnectionPoolSettings.class)))
        .andReturn(builder);
    expect(builder.socketSettings(unit.get(SocketSettings.class))).andReturn(builder);
    expect(builder.heartbeatSocketSettings(unit.get(SocketSettings.class))).andReturn(builder);
    expect(builder.serverSettings(unit.get(ServerSettings.class))).andReturn(builder);
    expect(builder.sslSettings(unit.get(SslSettings.class))).andReturn(builder);

    unit.registerMock(MongoClientSettings.Builder.class, builder);

    MongoClientSettings settings = unit.mock(MongoClientSettings.class);
    expect(builder.build()).andReturn(settings);
    unit.registerMock(MongoClientSettings.class, settings);

    unit.mockStatic(MongoClientSettings.class);

    expect(MongoClientSettings.builder()).andReturn(builder);
  };

  private Block server = unit -> {
    ServerSettings settings = unit.mock(ServerSettings.class);
    unit.registerMock(ServerSettings.class, settings);

    ServerSettings.Builder builder = unit.mock(ServerSettings.Builder.class);
    expect(builder.build()).andReturn(settings);

    unit.mockStatic(ServerSettings.class);
    expect(ServerSettings.builder()).andReturn(builder);
  };

  private Block mongo = unit -> {
    unit.mockStatic(MongoClients.class);
    expect(MongoClients.create(unit.get(MongoClientSettings.class)))
        .andReturn(unit.get(MongoClient.class));
  };

  private Block env = unit -> {
    Router routes = unit.mock(Router.class);
    expect(routes.map(unit.capture(Route.Mapper.class))).andReturn(routes);

    Env env = unit.get(Env.class);
    expect(env.router()).andReturn(routes);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  private Block socket = unit -> {
    unit.mockStatic(SocketSettings.class);
  };

  private Block database = unit -> {
    MongoClient client = unit.get(MongoClient.class);
    expect(client.getDatabase("pets")).andReturn(unit.get(MongoDatabase.class));
  };

  @SuppressWarnings("unchecked")
  private Block collection = unit -> {
    MongoDatabase db = unit.get(MongoDatabase.class);
    expect(db.getCollection("Pets")).andReturn(unit.get(MongoCollection.class));
  };

  @Test
  public void configure() throws Exception {
    String db = "mongodb://localhost";
    new MockUnit(Env.class, Binder.class, MongoClient.class)
        .expect(instances(0))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class)))
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(env)
        .run(unit -> {
          new MongoRx()
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        });
  }

  @Test
  public void configure1() throws Exception {
    String db = "mongodb://localhost";
    new MockUnit(Env.class, Binder.class, MongoClient.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(env)
        .run(unit -> {
          new MongoRx()
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        });
  }

  @Test
  public void conf() throws Exception {
    assertEquals("mongodb://localhost", new MongoRx().config().getString("db"));
  }

  @Test
  public void onStop() throws Exception {
    String db = "mongodb://localhost";
    new MockUnit(Env.class, Binder.class, MongoClient.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(env)
        .expect(unit -> {
          MongoClient client = unit.get(MongoClient.class);
          client.close();
        })
        .run(unit -> {
          new MongoRx()
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void withObservable() throws Exception {
    String db = "mongodb://localhost/pets";
    new MockUnit(Env.class, Binder.class, MongoClient.class, MongoDatabase.class, Scheduler.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(database)
        .expect(bind(Key.get(MongoDatabase.class, Names.named("pets"))))
        .expect(unit -> {
          MongoDatabase database = unit.get(MongoDatabase.class);
          expect(database.withObservableAdapter(unit.capture(ObservableAdapter.class)))
              .andReturn(database);

          Observable observable = unit.powerMock(Observable.class);
          unit.registerMock(Observable.class, observable);
          expect(observable.observeOn(unit.get(Scheduler.class))).andReturn(observable);
        })
        .expect(env)
        .run(unit -> {
          new MongoRx()
              .observableAdapter(o -> o.observeOn(unit.get(Scheduler.class)))
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        }, unit -> {
          unit.captured(ObservableAdapter.class).iterator().next()
              .adapt(unit.get(Observable.class));
        });
  }

  @Test
  public void withCodecRegistry() throws Exception {
    String db = "mongodb://localhost/pets";
    new MockUnit(Env.class, Binder.class, MongoClient.class, MongoDatabase.class, CodecRegistry.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(database)
        .expect(bind(Key.get(MongoDatabase.class, Names.named("pets"))))
        .expect(unit -> {
          unit.get(CodecRegistry.class);
          MongoDatabase database = unit.get(MongoDatabase.class);
          expect(database.withCodecRegistry(unit.get(CodecRegistry.class))).andReturn(database);
        })
        .expect(env)
        .run(unit -> {
          new MongoRx()
              .codecRegistry(unit.get(CodecRegistry.class))
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        });
  }

  @Test
  public void withCallback() throws Exception {
    String db = "mongodb://localhost";
    new MockUnit(Env.class, Binder.class, MongoClient.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(env)
        .expect(unit -> {
          MongoClientSettings.Builder settings = unit.get(MongoClientSettings.Builder.class);
          expect(settings.readConcern(ReadConcern.LOCAL)).andReturn(settings);
        })
        .run(unit -> {
          new MongoRx()
              .doWith(settings -> {
                settings.readConcern(ReadConcern.LOCAL);
              })
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  @Test
  public void mongoRxMapper() throws Exception {
    String db = "mongodb://localhost";
    new MockUnit(Env.class, Binder.class, MongoClient.class, FindObservable.class,
        ListCollectionsObservable.class, ListDatabasesObservable.class, AggregateObservable.class,
        DistinctObservable.class, MapReduceObservable.class, MongoObservable.class)
            .expect(instances(1))
            .expect(cluster(db))
            .expect(pool(db))
            .expect(socket)
            .expect(socket(db))
            .expect(server)
            .expect(ssl(db))
            .expect(settings)
            .expect(mongo)
            .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
            .expect(env)
            .expect(unit -> {
              Observable observable = unit.powerMock(Observable.class);
              expect(observable.toList()).andReturn(unit.powerMock(Observable.class)).times(6);

              Observable mobservable = unit.powerMock(Observable.class);

              FindObservable o1 = unit.get(FindObservable.class);
              expect(o1.toObservable()).andReturn(observable);

              ListCollectionsObservable o2 = unit.get(ListCollectionsObservable.class);
              expect(o2.toObservable()).andReturn(observable);

              ListDatabasesObservable o3 = unit.get(ListDatabasesObservable.class);
              expect(o3.toObservable()).andReturn(observable);

              AggregateObservable o4 = unit.get(AggregateObservable.class);
              expect(o4.toObservable()).andReturn(observable);

              DistinctObservable o5 = unit.get(DistinctObservable.class);
              expect(o5.toObservable()).andReturn(observable);

              MapReduceObservable o6 = unit.get(MapReduceObservable.class);
              expect(o6.toObservable()).andReturn(observable);

              MongoObservable o7 = unit.get(MongoObservable.class);
              expect(o7.toObservable()).andReturn(mobservable);
            })
            .run(unit -> {
              new MongoRx()
                  .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
            }, unit -> {
              Route.Mapper mongorx = unit.captured(Route.Mapper.class).iterator().next();

              assertTrue(mongorx.map(unit.get(FindObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(ListCollectionsObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(ListDatabasesObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(AggregateObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(DistinctObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(MapReduceObservable.class)) instanceof Observable);
              assertTrue(
                  mongorx.map(unit.get(MongoObservable.class)) instanceof Observable);

              assertEquals("x", mongorx.map("x"));
            });
  }

  @Test
  public void withDatabase() throws Exception {
    String db = "mongodb://localhost/pets";
    new MockUnit(Env.class, Binder.class, MongoClient.class, MongoDatabase.class)
        .expect(instances(1))
        .expect(cluster(db))
        .expect(pool(db))
        .expect(socket)
        .expect(socket(db))
        .expect(server)
        .expect(ssl(db))
        .expect(settings)
        .expect(mongo)
        .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
        .expect(database)
        .expect(bind(Key.get(MongoDatabase.class, Names.named("pets"))))
        .expect(env)
        .run(unit -> {
          new MongoRx()
              .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
        });
  }

  @Test
  public void withCollection() throws Exception {
    String db = "mongodb://localhost/pets.Pets";
    new MockUnit(Env.class, Binder.class, MongoClient.class, MongoDatabase.class,
        MongoCollection.class)
            .expect(instances(1))
            .expect(cluster(db))
            .expect(pool(db))
            .expect(socket)
            .expect(socket(db))
            .expect(server)
            .expect(ssl(db))
            .expect(settings)
            .expect(mongo)
            .expect(bind(Key.get(MongoClient.class, Names.named("db"))))
            .expect(database)
            .expect(bind(Key.get(MongoDatabase.class, Names.named("pets"))))
            .expect(collection)
            .expect(bind(Key.get(MongoCollection.class, Names.named("Pets"))))
            .expect(env)
            .run(unit -> {
              new MongoRx()
                  .configure(unit.get(Env.class), conf(null, "db", db), unit.get(Binder.class));
            });
  }

  @Test
  public void withDirectDb() throws Exception {
    String db = "mongodb://localhost/pets.Pets";
    new MockUnit(Env.class, Binder.class, MongoClient.class, MongoDatabase.class,
        MongoCollection.class)
            .expect(instances(1))
            .expect(cluster(db))
            .expect(pool(db))
            .expect(socket)
            .expect(socket(db))
            .expect(server)
            .expect(ssl(db))
            .expect(settings)
            .expect(mongo)
            .expect(bind(Key.get(MongoClient.class, Names.named(db))))
            .expect(database)
            .expect(bind(Key.get(MongoDatabase.class, Names.named("pets"))))
            .expect(collection)
            .expect(bind(Key.get(MongoCollection.class, Names.named("Pets"))))
            .expect(env)
            .run(unit -> {
              new MongoRx(db)
                  .configure(unit.get(Env.class), conf(null), unit.get(Binder.class));
            });
  }

  private Block instances(final int n) {
    return unit -> {
      Field field = MongoRx.class.getDeclaredField("instances");
      field.setAccessible(true);
      AtomicInteger instances = (AtomicInteger) field.get(null);
      instances.set(n);
    };
  }

  @SuppressWarnings({"unchecked", "rawtypes" })
  private Block bind(final Key key) {
    return unit -> {
      Binder binder = unit.get(Binder.class);
      LinkedBindingBuilder bb = unit.mock(LinkedBindingBuilder.class);
      bb.toInstance(unit.get(key.getTypeLiteral().getRawType()));
      expect(binder.bind(key)).andReturn(bb);
    };
  }

  private Block cluster(final String db) {
    return unit -> {
      ClusterSettings settings = unit.mock(ClusterSettings.class);
      unit.registerMock(ClusterSettings.class, settings);

      ClusterSettings.Builder builder = unit.mock(ClusterSettings.Builder.class);
      expect(builder.applyConnectionString(new ConnectionString(db))).andReturn(builder);
      expect(builder.build()).andReturn(settings);

      unit.mockStatic(ClusterSettings.class);
      expect(ClusterSettings.builder()).andReturn(builder);
    };
  }

  private Block pool(final String db) {
    return unit -> {
      ConnectionPoolSettings settings = unit.mock(ConnectionPoolSettings.class);
      unit.registerMock(ConnectionPoolSettings.class, settings);

      ConnectionPoolSettings.Builder builder = unit.mock(ConnectionPoolSettings.Builder.class);
      expect(builder.applyConnectionString(new ConnectionString(db))).andReturn(builder);
      expect(builder.build()).andReturn(settings);

      unit.mockStatic(ConnectionPoolSettings.class);
      expect(ConnectionPoolSettings.builder()).andReturn(builder);
    };
  }

  private Block socket(final String db) {
    return unit -> {
      SocketSettings settings = Try.of(() -> unit.get(SocketSettings.class))
          .getOrElse(() -> unit.mock(SocketSettings.class));
      unit.registerMock(SocketSettings.class, settings);

      SocketSettings.Builder builder = Try.of(() -> unit.get(SocketSettings.Builder.class))
          .getOrElse(() -> unit.mock(SocketSettings.Builder.class));
      expect(builder.applyConnectionString(new ConnectionString(db))).andReturn(builder).times(2);
      expect(builder.build()).andReturn(settings).times(2);

      expect(SocketSettings.builder()).andReturn(builder).times(2);
    };
  }

  private Block ssl(final String db) {
    return unit -> {
      SslSettings settings = unit.mock(SslSettings.class);
      unit.registerMock(SslSettings.class, settings);

      SslSettings.Builder builder = unit.mock(SslSettings.Builder.class);
      expect(builder.applyConnectionString(new ConnectionString(db))).andReturn(builder);
      expect(builder.build()).andReturn(settings);

      unit.mockStatic(SslSettings.class);
      expect(SslSettings.builder()).andReturn(builder);
    };
  }

  @Test
  public void cluster() {
    ClusterSettings cluster = MongoRx.cluster(new ConnectionString("mongodb://localhost"),
        conf("cluster",
            "maxWaitQueueSize", 5,
            "replicaSetName", "r",
            "requiredClusterType", "replica_set",
            "serverSelectionTimeout", "3s"));

    assertEquals(5, cluster.getMaxWaitQueueSize());
    assertEquals("r", cluster.getRequiredReplicaSetName());
    assertEquals(ClusterType.REPLICA_SET, cluster.getRequiredClusterType());
    assertEquals(3, cluster.getServerSelectionTimeout(TimeUnit.SECONDS));
  }

  @Test
  public void pool() {
    ConnectionPoolSettings pool = MongoRx.pool(new ConnectionString("mongodb://localhost"),
        conf("pool",
            "maintenanceFrequency", "1s",
            "maintenanceInitialDelay", "2s",
            "maxConnectionIdleTime", "3s",
            "maxConnectionLifeTime", 4500,
            "maxSize", 3,
            "maxWaitQueueSize", 9,
            "maxWaitTime", "1m",
            "minSize", 1));

    assertEquals(1, pool.getMaintenanceFrequency(TimeUnit.SECONDS));
    assertEquals(2, pool.getMaintenanceInitialDelay(TimeUnit.SECONDS));
    assertEquals(3, pool.getMaxConnectionIdleTime(TimeUnit.SECONDS));
    assertEquals(4500, pool.getMaxConnectionLifeTime(TimeUnit.MILLISECONDS));
    assertEquals(60, pool.getMaxWaitTime(TimeUnit.SECONDS));
    assertEquals(3, pool.getMaxSize());
    assertEquals(9, pool.getMaxWaitQueueSize());
    assertEquals(1, pool.getMinSize());
  }

  @Test
  public void server() {
    ServerSettings server = MongoRx.server(
        conf("server",
            "heartbeatFrequency", "1s",
            "minHeartbeatFrequency", "2s"));

    assertEquals(1, server.getHeartbeatFrequency(TimeUnit.SECONDS));
    assertEquals(2, server.getMinHeartbeatFrequency(TimeUnit.SECONDS));
  }

  @Test
  public void socket() {
    SocketSettings socket = MongoRx.socket("socket", new ConnectionString("mongodb://localhost"),
        conf("socket",
            "connectTimeout", "1s",
            "readTimeout", "2s",
            "receiveBufferSize", 15,
            "sendBufferSize", 20));

    assertEquals(1, socket.getConnectTimeout(TimeUnit.SECONDS));
    assertEquals(2, socket.getReadTimeout(TimeUnit.SECONDS));
    assertEquals(15, socket.getReceiveBufferSize());
    assertEquals(20, socket.getSendBufferSize());
  }

  @Test
  public void ssl() {
    SslSettings ssl = MongoRx.ssl(new ConnectionString("mongodb://localhost"),
        conf("ssl",
            "enabled", true,
            "invalidHostNameAllowed", true));

    assertEquals(true, ssl.isEnabled());
    assertEquals(true, ssl.isInvalidHostNameAllowed());
  }

  @Test
  public void settings() {
    MongoClientSettings settings = MongoRx.settings(new ConnectionString("mongodb://localhost"),
        conf(null,
            "readConcern", "local",
            "readPreference", "primary",
            "writeConcern", "w3"))
        .build();

    assertEquals(ReadConcern.LOCAL, settings.getReadConcern());
    assertEquals(ReadPreference.primary(), settings.getReadPreference());
    assertEquals(WriteConcern.W3, settings.getWriteConcern());
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongReadConcern() {
    MongoRx.settings(new ConnectionString("mongodb://localhost"),
        conf(null,
            "readConcern", "Xxx",
            "readPreference", "primary",
            "writeConcern", "w3"))
        .build();
  }

  @Test(expected = IllegalArgumentException.class)
  public void wrongWriteConcern() {
    MongoRx.settings(new ConnectionString("mongodb://localhost"),
        conf(null,
            "readPreference", "primary",
            "writeConcern", "Xqw"))
        .build();
  }

  @Test
  public void dbconf() {
    assertEquals("default",
        MongoRx.dbconf("db", conf("mongo", "readConcern", "default")).getString("readConcern"));

    assertEquals("default",
        MongoRx.dbconf("mongodb://localhost", conf("mongo", "readConcern", "default"))
            .getString("readConcern"));

    assertEquals("local",
        MongoRx.dbconf("db",
            conf(null,
                "mongo.readConcern", "default",
                "mongo.db.readConcern", "local"))
            .getString("readConcern"));
  }

  private Config conf(final String scope, final Object... values) {
    ImmutableMap.Builder<String, Object> hash = ImmutableMap.builder();
    String prefix = scope == null ? "" : scope + ".";
    for (int i = 0; i < values.length; i += 2) {
      hash.put(prefix + values[i].toString(), values[i + 1]);
    }
    return ConfigFactory.parseMap(hash.build());
  }
}
