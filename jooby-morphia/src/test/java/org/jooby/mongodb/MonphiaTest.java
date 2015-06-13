package org.jooby.mongodb;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import javax.inject.Provider;

import org.jooby.Env;
import org.jooby.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.mapping.Mapper;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoDatabase;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Monphia.class, Morphia.class, Mapper.class, MongoClient.class,
    MongodbManaged.class })
public class MonphiaTest {

  private Config $mongodb = ConfigFactory.parseResources(Mongodb.class, "mongodb.conf");

  @SuppressWarnings("unchecked")
  MockUnit.Block mongodb = unit -> {
    ScopedBindingBuilder mcSBB = unit.mock(ScopedBindingBuilder.class);
    mcSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoClient> mcABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(mcABB.toProvider(unit.capture(MongodbManaged.class))).andReturn(mcSBB);

    ScopedBindingBuilder dbSBB = unit.mock(ScopedBindingBuilder.class);
    dbSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoDatabase> dbABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(dbABB.toProvider(isA(Provider.class))).andReturn(dbSBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(MongoClient.class))).andReturn(mcABB);

    expect(binder.bind(Key.get(MongoDatabase.class))).andReturn(dbABB);
  };

  @SuppressWarnings("unchecked")
  MockUnit.Block mongodbNamed = unit -> {
    ScopedBindingBuilder mcSBB = unit.mock(ScopedBindingBuilder.class);
    mcSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoClient> mcABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(mcABB.toProvider(isA(Provider.class))).andReturn(mcSBB);

    ScopedBindingBuilder dbSBB = unit.mock(ScopedBindingBuilder.class);
    dbSBB.asEagerSingleton();

    AnnotatedBindingBuilder<MongoDatabase> dbABB = unit.mock(AnnotatedBindingBuilder.class);
    expect(dbABB.toProvider(isA(Provider.class))).andReturn(dbSBB);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(MongoClient.class, Names.named("mydb")))).andReturn(mcABB);

    expect(binder.bind(Key.get(MongoDatabase.class, Names.named("mydb")))).andReturn(dbABB);
  };

  @SuppressWarnings("unchecked")
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
          Mapper mapper = unit.mockConstructor(Mapper.class);
          Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class },
              mapper);

          LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(morphia);

          LinkedBindingBuilder<GuiceObjectFactory> gofLBB = unit.mock(LinkedBindingBuilder.class);
          gofLBB.asEagerSingleton();

          ScopedBindingBuilder dsSBB = unit.mock(ScopedBindingBuilder.class);
          dsSBB.asEagerSingleton();

          LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
          expect(dsLBB.toProvider(isA(Provider.class))).andReturn(dsSBB);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(Morphia.class))).andReturn(mLBB);
          expect(binder.bind(Key.get(GuiceObjectFactory.class))).andReturn(gofLBB);
          expect(binder.bind(Key.get(Datastore.class))).andReturn(dsLBB);
        })
        .run(unit -> {
          new Monphia()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithName() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(mongodbNamed)
        .expect(unit -> {
          Mapper mapper = unit.mockConstructor(Mapper.class);
          Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class },
              mapper);

          LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(morphia);

          LinkedBindingBuilder<GuiceObjectFactory> gofLBB = unit
              .mock(LinkedBindingBuilder.class);
          gofLBB.asEagerSingleton();

          ScopedBindingBuilder dsSBB = unit.mock(ScopedBindingBuilder.class);
          dsSBB.asEagerSingleton();

          LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
          expect(dsLBB.toProvider(isA(Provider.class))).andReturn(dsSBB);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(Morphia.class, Names.named("mydb")))).andReturn(mLBB);
          expect(binder.bind(Key.get(GuiceObjectFactory.class, Names.named("mydb"))))
              .andReturn(gofLBB);
          expect(binder.bind(Key.get(Datastore.class, Names.named("mydb")))).andReturn(dsLBB);
        })
        .run(unit -> {
          new Monphia("db")
              .named()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithMorphiaCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("mongodb")).andReturn($mongodb.getConfig("mongodb"));
          expect(config.hasPath("mongodb.db")).andReturn(false);
          expect(config.getString("db")).andReturn("mongodb://127.0.0.1/mydb");
        })
        .expect(mongodb)
        .expect(unit -> {
          Mapper mapper = unit.mockConstructor(Mapper.class);

          Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class },
              mapper);
          expect(morphia.map(MonphiaTest.class)).andReturn(morphia);

          LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(morphia);

          LinkedBindingBuilder<GuiceObjectFactory> gofLBB = unit.mock(LinkedBindingBuilder.class);
          gofLBB.asEagerSingleton();

          ScopedBindingBuilder dsSBB = unit.mock(ScopedBindingBuilder.class);
          dsSBB.asEagerSingleton();

          LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
          expect(dsLBB.toProvider(isA(Provider.class))).andReturn(dsSBB);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(Morphia.class))).andReturn(mLBB);
          expect(binder.bind(Key.get(GuiceObjectFactory.class))).andReturn(gofLBB);
          expect(binder.bind(Key.get(Datastore.class))).andReturn(dsLBB);
        })
        .run(unit -> {
          new Monphia()
              .doWith((morphia, config) -> {
                morphia.map(MonphiaTest.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithoutDatastoreCallback() throws Exception {
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

          Datastore ds = unit.mock(Datastore.class);

          Mapper mapper = unit.mockConstructor(Mapper.class);

          Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class },
              mapper);
          expect(morphia.createDatastore(client, "mydb")).andReturn(ds);

          LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(morphia);

          LinkedBindingBuilder<GuiceObjectFactory> gofLBB = unit
              .mock(LinkedBindingBuilder.class);
          gofLBB.asEagerSingleton();

          ScopedBindingBuilder dsSBB = unit.mock(ScopedBindingBuilder.class);
          dsSBB.asEagerSingleton();

          LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
          expect(dsLBB.toProvider(unit.capture(Provider.class))).andReturn(dsSBB);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(Morphia.class))).andReturn(mLBB);
          expect(binder.bind(Key.get(GuiceObjectFactory.class))).andReturn(gofLBB);
          expect(binder.bind(Key.get(Datastore.class))).andReturn(dsLBB);
        })
        .run(unit -> {
          new Monphia()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(MongodbManaged.class).iterator().next().start();

          unit.captured(Provider.class).forEach(Provider::get);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsWithDatastoreCallback() throws Exception {
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

          Datastore ds = unit.mock(Datastore.class);
          ds.ensureIndexes();

          Mapper mapper = unit.mockConstructor(Mapper.class);

          Morphia morphia = unit.mockConstructor(Morphia.class, new Class[]{Mapper.class },
              mapper);
          expect(morphia.createDatastore(client, "mydb")).andReturn(ds);

          LinkedBindingBuilder<Morphia> mLBB = unit.mock(LinkedBindingBuilder.class);
          mLBB.toInstance(morphia);

          LinkedBindingBuilder<GuiceObjectFactory> gofLBB = unit
              .mock(LinkedBindingBuilder.class);
          gofLBB.asEagerSingleton();

          ScopedBindingBuilder dsSBB = unit.mock(ScopedBindingBuilder.class);
          dsSBB.asEagerSingleton();

          LinkedBindingBuilder<Datastore> dsLBB = unit.mock(LinkedBindingBuilder.class);
          expect(dsLBB.toProvider(unit.capture(Provider.class))).andReturn(dsSBB);

          Binder binder = unit.get(Binder.class);

          expect(binder.bind(Key.get(Morphia.class))).andReturn(mLBB);
          expect(binder.bind(Key.get(GuiceObjectFactory.class))).andReturn(gofLBB);
          expect(binder.bind(Key.get(Datastore.class))).andReturn(dsLBB);
        })
        .run(unit -> {
          new Monphia()
              .doWith(ds -> {
                ds.ensureIndexes();
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(MongodbManaged.class).iterator().next().start();

          unit.captured(Provider.class).forEach(Provider::get);
        });
  }
}
