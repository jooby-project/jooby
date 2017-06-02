package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Mongodb.class, MongoClient.class })
public class MongodbTest {

  private Config $mongodb = ConfigFactory.parseResources(getClass(), "mongodb.conf");

  @SuppressWarnings("unchecked")
  MockUnit.Block mongodb = unit -> {
    AnnotatedBindingBuilder<MongoClientURI> mcuABB = unit.mock(AnnotatedBindingBuilder.class);
    mcuABB.toInstance(isA(MongoClientURI.class));
    mcuABB.toInstance(isA(MongoClientURI.class));

    MongoClient client = unit.constructor(MongoClient.class)
        .args(MongoClientURI.class)
        .build(isA(MongoClientURI.class));

    MongoDatabase db = unit.mock(MongoDatabase.class);
    expect(client.getDatabase("mydb")).andReturn(db);

    unit.registerMock(MongoClient.class, client);

    AnnotatedBindingBuilder<MongoClient> mcABB = unit.mock(AnnotatedBindingBuilder.class);
    mcABB.toInstance(client);
    mcABB.toInstance(client);

    AnnotatedBindingBuilder<MongoDatabase> dbABB = unit.mock(AnnotatedBindingBuilder.class);
    dbABB.toInstance(db);
    dbABB.toInstance(db);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(MongoClientURI.class))).andReturn(mcuABB);
    expect(binder.bind(Key.get(MongoClientURI.class, Names.named("mydb")))).andReturn(mcuABB);

    expect(binder.bind(Key.get(MongoClient.class))).andReturn(mcABB);
    expect(binder.bind(Key.get(MongoClient.class, Names.named("mydb")))).andReturn(mcABB);

    expect(binder.bind(Key.get(MongoDatabase.class))).andReturn(dbABB);
    expect(binder.bind(Key.get(MongoDatabase.class, Names.named("mydb")))).andReturn(dbABB);

    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .expect(unit-> {
          MongoClient client = unit.get(MongoClient.class);
          client.close();
        })
        .run(unit -> {
          Mongodb mongodb = new Mongodb();
          mongodb.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedRunnable.class).iterator().next().run();
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFaileWhenDbIsMissing() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1");
        })
        .expect(mongodb)
        .run(unit -> {
          Mongodb mongodb = new Mongodb();
          mongodb.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsWithCustomOption() throws Exception {

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb"))
              .andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(true);
          expect(config.getConfig("mongodb.db")).andReturn(ConfigFactory.empty()
              .withValue("connectionsPerHost", ConfigValueFactory.fromAnyRef(50)));
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .run(unit -> {
          new Mongodb()
              .options((options, config) -> {
                assertEquals(50, options.build().getConnectionsPerHost());
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsConfig() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .run(unit -> {
          assertEquals($mongodb, new Mongodb().config());
        });
  }

  @Test
  public void defaultsWithOptions() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .run(unit -> {
          new Mongodb()
              .options((options, config) -> {
                options.connectTimeout(3000);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block serviceKey(final ServiceKey serviceKey) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(serviceKey);
    };
  }

}
