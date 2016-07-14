package org.jooby.couchbase;

import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jooby.Env;
import org.jooby.Registry;
import org.jooby.internal.couchbase.AsyncDatastoreImpl;
import org.jooby.internal.couchbase.DatastoreImpl;
import org.jooby.internal.couchbase.IdGenerator;
import org.jooby.internal.couchbase.JacksonMapper;
import org.jooby.internal.couchbase.SetConverterHack;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.couchbase.client.java.AsyncBucket;
import com.couchbase.client.java.Bucket;
import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.cluster.ClusterManager;
import com.couchbase.client.java.document.JsonLongDocument;
import com.couchbase.client.java.env.CouchbaseEnvironment;
import com.couchbase.client.java.env.DefaultCouchbaseEnvironment;
import com.couchbase.client.java.repository.AsyncRepository;
import com.couchbase.client.java.repository.Repository;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

import javaslang.control.Try.CheckedConsumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest(value = {DefaultCouchbaseEnvironment.class, CouchbaseCluster.class,
    SetConverterHack.class, AsyncDatastoreImpl.class, DatastoreImpl.class, System.class,
    IdGenerator.class },
    fullyQualifiedNames = "org.jooby.couchbase.*")
public class CouchbaseTest {

  private Block createEnv = unit -> {
    DefaultCouchbaseEnvironment env = unit.mock(DefaultCouchbaseEnvironment.class);
    unit.registerMock(CouchbaseEnvironment.class, env);

    unit.mockStatic(DefaultCouchbaseEnvironment.class);
    expect(DefaultCouchbaseEnvironment.create()).andReturn(env);
  };

  private Block noenvprops = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.hasPath("couchbase.env")).andReturn(false);
  };

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block bindEnv = unit -> {
    CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);

    AnnotatedBindingBuilder abbce = unit.mock(AnnotatedBindingBuilder.class);
    abbce.toInstance(env);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(CouchbaseEnvironment.class)).andReturn(abbce);
  };

  private Block noClusterManager = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.hasPath("couchbase.cluster.username")).andReturn(false);
  };

  private Block converterHack = unit -> {
    unit.mockStatic(SetConverterHack.class);

    SetConverterHack.forceConverter(unit.get(AsyncRepository.class), Couchbase.CONVERTER);
  };

  @SuppressWarnings("unchecked")
  private Block onStop = unit -> {
    Env env = unit.get(Env.class);

    expect(env.onStop(unit.capture(CheckedConsumer.class))).andReturn(env);
  };

  @Test
  public void config() throws Exception {
    assertEquals(ConfigFactory.parseResources(Couchbase.class, "couchbase.conf"),
        new Couchbase().config());
  }

  @Test
  public void boot() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void envproperty() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    Config conf = ConfigFactory.empty()
        .withValue("couchbase.env.kvEndpoints", ConfigValueFactory.fromAnyRef(5));
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(setProperty(bucket))
        .expect(unit -> {
          Config mock = unit.get(Config.class);
          expect(mock.hasPath("couchbase.env")).andReturn(true);
          expect(mock.getConfig("couchbase.env")).andReturn(conf.getConfig("couchbase.env"));

          expect(System.setProperty("com.couchbase.kvEndpoints", "5")).andReturn(null);
        })
        .expect(createEnv)
        .expect(bindEnv)
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void twoOrMoreClusterShouldNotBindEnv() throws Exception {
    Couchbase.COUNTER.set(1);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, CouchbaseEnvironment.class)
        .expect(noenvprops)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .environment(unit.get(CouchbaseEnvironment.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });

  }

  @Test
  public void withDbProperty() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind("db", CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.getString("db")).andReturn("couchbase://localhost/" + bucket);
        })
        .run(unit -> {
          new Couchbase("db")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void sessionBucket() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket("session", null, false))
        .expect(bucketPassword("session", null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .sessionBucket("session")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void customEnv() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, CouchbaseEnvironment.class)
        .expect(noenvprops)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .environment(unit.get(CouchbaseEnvironment.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void bootIdGen() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    Object bean = new Object();
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          unit.mockStatic(IdGenerator.class);

          expect(IdGenerator.getOrGenId(eq(bean), unit.capture(Supplier.class))).andReturn(13L);

          Bucket b = unit.first(Bucket.class);
          JsonLongDocument json = JsonLongDocument.create("1", 2L);
          expect(b.counter(bean.getClass().getName(), 1, 1)).andReturn(json);
        })
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          assertEquals(13L, unit.captured(Function.class).iterator().next().apply(bean));
          assertEquals(2L, unit.captured(Supplier.class).iterator().next().get());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStopNormal() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          Bucket b = unit.get(Bucket.class);
          expect(b.close()).andReturn(true);

          Registry r = unit.get(Registry.class);
          expect(r.require(bucket, Bucket.class)).andReturn(b);

          CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
          expect(cluster.disconnect()).andReturn(true);

          CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);
          expect(env.shutdown()).andReturn(true);
        })
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStopBucketErr() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          Bucket b = unit.get(Bucket.class);
          expect(b.close()).andThrow(new IllegalStateException("intentional err"));

          Registry r = unit.get(Registry.class);
          expect(r.require(bucket, Bucket.class)).andReturn(b);

          CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
          expect(cluster.disconnect()).andReturn(true);

          CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);
          expect(env.shutdown()).andReturn(true);
        })
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStopClusterErr() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          Bucket b = unit.get(Bucket.class);
          expect(b.close()).andReturn(true);

          Registry r = unit.get(Registry.class);
          expect(r.require(bucket, Bucket.class)).andReturn(b);

          CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
          expect(cluster.disconnect()).andThrow(new IllegalStateException("intentional err"));

          CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);
          expect(env.shutdown()).andReturn(true);
        })
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void onStopEnvErr() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class, Registry.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .expect(unit -> {
          Bucket b = unit.get(Bucket.class);
          expect(b.close()).andReturn(true);

          Registry r = unit.get(Registry.class);
          expect(r.require(bucket, Bucket.class)).andReturn(b);

          CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
          expect(cluster.disconnect()).andReturn(true);

          CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);
          expect(env.shutdown()).andThrow(new IllegalStateException("intentional err"));
        })
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(CheckedConsumer.class).iterator().next().accept(unit.get(Registry.class));
        });
  }

  @Test
  public void multipleBuckets() throws Exception {
    Couchbase.COUNTER.set(0);
    String b1 = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(b1))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(b1, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(b1, null))
        .expect(openBucket(b1, null))
        .expect(bind(b1, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(b1, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(b1))
        .expect(bind(b1, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(b1, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(b1, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(b1, Datastore.class))
        .expect(bind(null, Datastore.class))
        // foo bucket
        .expect(bucketPassword("foo", null))
        .expect(openBucket("foo", null))
        .expect(bind("foo", Bucket.class))
        .expect(bind("foo", AsyncBucket.class))
        // repository
        .expect(repository("foo"))
        .expect(bind("foo", Repository.class))
        .expect(bind("foo", AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind("foo", AsyncDatastore.class))
        .expect(ds())
        .expect(bind("foo", Datastore.class))
        // session
        .expect(openBucket(b1, null, false))
        .expect(bucketPassword(b1, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + b1)
              .buckets("foo")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void bucketWithPassword() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, "bar"))
        .expect(openBucket(bucket, "bar"))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void defaultcluster() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "default";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        .expect(noClusterManager)
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost")
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void clusterManger() throws Exception {
    Couchbase.COUNTER.set(0);
    String bucket = "beers";
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(noenvprops)
        .expect(createEnv)
        .expect(bindEnv)
        .expect(setProperty(bucket))
        .expect(cluster("couchbase://localhost"))
        .expect(bind(bucket, CouchbaseCluster.class))
        .expect(bind(null, CouchbaseCluster.class))
        // cluster manager
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.hasPath("couchbase.cluster.username")).andReturn(true);
          expect(conf.getString("couchbase.cluster.username")).andReturn("usr");
          expect(conf.getString("couchbase.cluster.password")).andReturn("pwd");

          ClusterManager manager = unit.mock(ClusterManager.class);
          unit.registerMock(ClusterManager.class, manager);

          CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
          expect(cluster.clusterManager("usr", "pwd")).andReturn(manager);
        })
        .expect(bind("beers", ClusterManager.class))
        .expect(bind(null, ClusterManager.class))
        // buckets
        .expect(bucketPassword(bucket, null))
        .expect(openBucket(bucket, null))
        .expect(bind(bucket, Bucket.class))
        .expect(bind(null, Bucket.class))
        .expect(bind(bucket, AsyncBucket.class))
        .expect(bind(null, AsyncBucket.class))
        // repository
        .expect(repository(bucket))
        .expect(bind(bucket, Repository.class))
        .expect(bind(null, Repository.class))
        .expect(bind(bucket, AsyncRepository.class))
        .expect(bind(null, AsyncRepository.class))
        // converter hack
        .expect(converterHack)
        // datastore
        .expect(asyncds())
        .expect(bind(bucket, AsyncDatastore.class))
        .expect(bind(null, AsyncDatastore.class))
        .expect(ds())
        .expect(bind(bucket, Datastore.class))
        .expect(bind(null, Datastore.class))
        // session
        .expect(openBucket(bucket, null, false))
        .expect(bucketPassword(bucket, null))
        .expect(bind("session", Bucket.class))
        // onStop
        .expect(onStop)
        .run(unit -> {
          new Couchbase("couchbase://localhost/" + bucket)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block setProperty(final String value) {
    return unit -> {
      unit.mockStatic(System.class);
      expect(System.setProperty(N1Q.COUCHBASE_DEFBUCKET, value)).andReturn(value);
    };
  }

  private Block asyncds() {
    return unit -> {
      AsyncDatastoreImpl asyncds = unit.constructor(AsyncDatastoreImpl.class)
          .args(AsyncBucket.class, AsyncRepository.class, Function.class, JacksonMapper.class)
          .build(eq(unit.get(AsyncBucket.class)), eq(unit.get(AsyncRepository.class)),
              unit.capture(Function.class), eq(Couchbase.CONVERTER));
      unit.registerMock(AsyncDatastore.class, asyncds);
    };
  }

  private Block ds() {
    return unit -> {
      AsyncDatastore async = unit.get(AsyncDatastore.class);
      Datastore ds = unit.constructor(DatastoreImpl.class)
          .args(AsyncDatastore.class)
          .build(async);
      unit.registerMock(Datastore.class, ds);
    };
  }

  private Block repository(final String name) {
    return unit -> {
      Repository repo = unit.mock(Repository.class);
      unit.registerMock(Repository.class, repo);

      Bucket bucket = unit.get(Bucket.class);
      expect(bucket.repository()).andReturn(repo);

      AsyncRepository arepo = unit.mock(AsyncRepository.class);
      expect(repo.async()).andReturn(arepo);
      unit.registerMock(AsyncRepository.class, arepo);
    };
  }

  private Block openBucket(final String name, final String password) {
    return openBucket(name, password, true);
  }

  private Block openBucket(final String name, final String password, final boolean goasync) {
    return unit -> {
      Bucket bucket = unit.mock(Bucket.class);
      unit.registerMock(Bucket.class, bucket);
      if (goasync) {
        AsyncBucket asyncBucket = unit.mock(AsyncBucket.class);
        unit.registerMock(AsyncBucket.class, asyncBucket);
        expect(bucket.async()).andReturn(asyncBucket);
      }

      CouchbaseCluster cluster = unit.get(CouchbaseCluster.class);
      expect(cluster.openBucket(name, password)).andReturn(bucket);
    };
  }

  private Block bucketPassword(final String bucket, final String password) {
    return unit -> {
      Config conf = unit.get(Config.class);
      if (password == null) {
        expect(conf.hasPath("couchbase.bucket." + bucket + ".password")).andReturn(false);
        expect(conf.hasPath("couchbase.bucket.password")).andReturn(false);
      } else {
        expect(conf.hasPath("couchbase.bucket." + bucket + ".password")).andReturn(false);
        expect(conf.hasPath("couchbase.bucket.password")).andReturn(true);
        expect(conf.getString("couchbase.bucket.password")).andReturn(password);
      }
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block bind(final String name, final Class type) {
    return unit -> {
      Object value = unit.get(type);

      AnnotatedBindingBuilder abbce = unit.mock(AnnotatedBindingBuilder.class);
      abbce.toInstance(value);

      Binder binder = unit.get(Binder.class);
      if (name == null) {
        expect(binder.bind(Key.get(type))).andReturn(abbce);
      } else {
        expect(binder.bind(Key.get(type, Names.named(name)))).andReturn(abbce);
      }
    };
  }

  private Block cluster(final String string) {
    return unit -> {
      CouchbaseEnvironment env = unit.get(CouchbaseEnvironment.class);

      unit.mockStatic(CouchbaseCluster.class);

      CouchbaseCluster cluster = unit.mock(CouchbaseCluster.class);
      unit.registerMock(CouchbaseCluster.class, cluster);

      expect(CouchbaseCluster.fromConnectionString(env, string)).andReturn(cluster);
    };
  }
}
