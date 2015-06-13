package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Mongodb.class, MongodbManaged.class, MongoClient.class })
public class MongodbTest {

  private Config $mongodb = ConfigFactory.parseResources(getClass(), "mongodb.conf");

  @SuppressWarnings("unchecked")
  MockUnit.Block mongodb = unit -> {
    ScopedBindingBuilder mcSBB = unit.mock(ScopedBindingBuilder.class);
    mcSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoClient> mcABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(mcABB.toProvider(unit.capture(MongodbManaged.class))).andReturn(mcSBB);

    ScopedBindingBuilder dbSBB = unit.mock(ScopedBindingBuilder.class);
    dbSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoDatabase> dbABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(dbABB.toProvider(unit.capture(Provider.class))).andReturn(dbSBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(MongoClient.class))).andReturn(mcABB);

    expect(binder.bind(Key.get(MongoDatabase.class))).andReturn(dbABB);
  };

  @SuppressWarnings("unchecked")
  MockUnit.Block mongodbNamed = unit -> {
    ScopedBindingBuilder mcSBB = unit.mock(ScopedBindingBuilder.class);
    mcSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoClient> mcABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(mcABB.toProvider(unit.capture(MongodbManaged.class))).andReturn(mcSBB);

    ScopedBindingBuilder dbSBB = unit.mock(ScopedBindingBuilder.class);
    dbSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoDatabase> dbABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(dbABB.toProvider(unit.capture(Provider.class))).andReturn(dbSBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(MongoClient.class, Names.named("mydb")))).andReturn(mcABB);

    expect(binder.bind(Key.get(MongoDatabase.class, Names.named("mydb")))).andReturn(dbABB);
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
        .expect(mongodb)
        .expect(unit -> {
          MongoClient client = unit.mockConstructor(MongoClient.class,
              new Class[]{MongoClientURI.class }, isA(MongoClientURI.class));

          MongoDatabase db = unit.mock(MongoDatabase.class);
          expect(client.getDatabase("mydb")).andReturn(db);
        })
        .run(unit -> {
          Mongodb mongodb = new Mongodb();
          mongodb.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(MongodbManaged.class).iterator().next().start();
          assertNotNull(unit.captured(Provider.class).iterator().next().get());
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
        .expect(mongodb)
        .run(unit -> {
          new Mongodb()
              .options((options, config) -> {
                options.connectTimeout(3000);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsNamed() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(mongodbNamed)
        .run(unit -> {
          new Mongodb()
              .named()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsWithName() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.xdb")).andReturn(false);
          expect(config.getString("xdb")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(mongodbNamed)
        .run(unit -> {
          new Mongodb("xdb")
              .named()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

}
