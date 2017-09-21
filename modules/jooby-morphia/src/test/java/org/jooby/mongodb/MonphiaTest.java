package org.jooby.mongodb;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Registry;
import org.jooby.internal.mongodb.AutoIncID;
import org.jooby.internal.mongodb.GuiceObjectFactory;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Monphia.class, Morphia.class, Mapper.class, MongoClient.class, AutoIncID.class})
public class MonphiaTest {

  private Config $mongodb = ConfigFactory.parseResources(Mongodb.class, "mongodb.conf");

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
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  private Block objectFactory = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(env);
  };

  @SuppressWarnings("unchecked")
  private Block morphia = unit -> {
    MongoClient client = unit.get(MongoClient.class);

    Datastore ds = unit.mock(Datastore.class);
    unit.registerMock(Datastore.class, ds);

    Mapper mapper = unit.mockConstructor(Mapper.class);
    unit.registerMock(Mapper.class, mapper);

    Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class},
        mapper);
    unit.registerMock(Morphia.class, morphia);

    expect(morphia.createDatastore(client, mapper, "mydb")).andReturn(ds);

    LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
    mLBB.toInstance(morphia);
    mLBB.toInstance(morphia);

    LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
    dsLBB.toInstance(ds);
    dsLBB.toInstance(ds);

    Binder binder = unit.get(Binder.class);

    expect(binder.bind(Key.get(Morphia.class))).andReturn(mLBB);
    expect(binder.bind(Key.get(Datastore.class))).andReturn(dsLBB);

    expect(binder.bind(Key.get(Morphia.class, Names.named("mydb")))).andReturn(mLBB);
    expect(binder.bind(Key.get(Datastore.class, Names.named("mydb")))).andReturn(dsLBB);
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
        .expect(objectFactory)
        .expect(morphia)
        .run(unit -> {
          new Monphia()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStart() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .expect(objectFactory)
        .expect(morphia)
        .expect(unit -> {
          unit.constructor(GuiceObjectFactory.class)
              .build(unit.get(Registry.class), unit.get(Morphia.class));
        })
        .run(unit -> {
          new Monphia("db")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  @Test
  public void defaultsWithMorphiaCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .expect(morphia)
        .expect(unit -> {
          Morphia morphia = unit.get(Morphia.class);
          expect(morphia.map(MonphiaTest.class)).andReturn(morphia);
        })
        .expect(objectFactory)
        .run(unit -> {
          new Monphia()
              .doWith((morphia, config) -> {
                morphia.map(MonphiaTest.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultsWithDatastoreCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .expect(morphia)
        .expect(unit -> {
          Datastore ds = unit.get(Datastore.class);
          ds.ensureIndexes();
        })
        .expect(objectFactory)
        .run(unit -> {
          new Monphia()
              .doWith(ds -> {
                ds.ensureIndexes();
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void withIdGen() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(serviceKey(new ServiceKey()))
        .expect(mongodb)
        .expect(objectFactory)
        .expect(morphia)
        .expect(unit -> {
          Datastore ds = unit.get(Datastore.class);
          ds.ensureIndexes();

          AutoIncID inc = unit.constructor(AutoIncID.class)
              .args(Datastore.class, IdGen.class)
              .build(ds, IdGen.GLOBAL);

          Mapper mapper = unit.get(Mapper.class);
          mapper.addInterceptor(inc);
        })
        .run(unit -> {
          new Monphia().with(IdGen.GLOBAL).doWith(ds -> {
            ds.ensureIndexes();
          }).configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block serviceKey(final ServiceKey serviceKey) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(serviceKey).times(2);
    };
  }

}
