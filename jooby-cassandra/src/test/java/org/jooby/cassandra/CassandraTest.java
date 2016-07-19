package org.jooby.cassandra;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import org.jooby.Env;
import org.jooby.Routes;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.Host.StateListener;
import com.datastax.driver.core.Session;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalDateCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalTimeCodec;
import com.datastax.driver.mapping.MappingManager;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;

import javaslang.control.Try.CheckedRunnable;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cassandra.class, Cluster.class, Cluster.Builder.class, CodecRegistry.class,
    MappingManager.class, Datastore.class, CassandraMapper.class })
public class CassandraTest {

  private Block clusterBuilder = unit -> {
    unit.mockStatic(Cluster.class);

    Cluster cluster = unit.get(Cluster.class);
    expect(cluster.getConfiguration()).andReturn(unit.get(Configuration.class));

    Builder builder = unit.get(Cluster.Builder.class);
    expect(Cluster.builder()).andReturn(builder);
    expect(builder.build()).andReturn(cluster);
  };

  private Block codecRegistry = unit -> {
    CodecRegistry codecRegistry = unit.powerMock(CodecRegistry.class);

    expect(codecRegistry.register(
        InstantCodec.instance,
        LocalDateCodec.instance,
        LocalTimeCodec.instance)).andReturn(codecRegistry);

    Configuration configuration = unit.get(Configuration.class);
    expect(configuration.getCodecRegistry()).andReturn(codecRegistry);
  };

  private Block mapper = unit -> {
    Session session = unit.get(Session.class);
    MappingManager manager = unit.constructor(MappingManager.class)
        .args(Session.class)
        .build(session);

    unit.registerMock(MappingManager.class, manager);
  };

  private Block datastore = unit -> {
    MappingManager manager = unit.get(MappingManager.class);
    Datastore ds = unit.constructor(Datastore.class)
        .args(MappingManager.class)
        .build(manager);

    unit.registerMock(Datastore.class, ds);
  };

  private Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  private Block routeMapper = unit -> {
    CassandraMapper mapper = unit.constructor(CassandraMapper.class)
        .build();

    Routes routes = unit.mock(Routes.class);
    expect(routes.map(mapper)).andReturn(routes);

    Env env = unit.get(Env.class);
    expect(env.routes()).andReturn(routes);
  };

  @Test
  public void connectViaProperty() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(unit -> {
              Config conf = unit.get(Config.class);
              expect(conf.getString("db")).andReturn("cassandra://localhost/beers");
            })
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .run(unit -> {
              new Cassandra()
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void connectViaConnectionString() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void onStop() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .expect(unit -> {
              Session session = unit.get(Session.class);
              session.close();

              Cluster cluster = unit.get(Cluster.class);
              cluster.close();
            })
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              unit.captured(CheckedRunnable.class).iterator().next().run();
            });
  }

  @Test
  public void onStopSessionerr() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .expect(unit -> {
              Session session = unit.get(Session.class);
              session.close();
              expectLastCall().andThrow(new IllegalStateException("intentional err"));

              Cluster cluster = unit.get(Cluster.class);
              cluster.close();
            })
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            }, unit -> {
              unit.captured(CheckedRunnable.class).iterator().next().run();
            });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withAccessor() throws Exception {
    Cassandra.COUNTER.set(0);
    Object value = new Object();
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .expect(unit -> {
              MappingManager manager = unit.get(MappingManager.class);
              expect(manager.createAccessor(Object.class)).andReturn(value);

              AnnotatedBindingBuilder<Object> abb = unit.mock(AnnotatedBindingBuilder.class);
              abb.toInstance(value);

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(Object.class)).andReturn(abb);
            })
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .accesor(Object.class)
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void doWithCluster() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class, StateListener.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(routeMapper).expect(onStop)
            .expect(unit -> {
              Cluster cluster = unit.get(Cluster.class);
              expect(cluster.register(unit.get(StateListener.class))).andReturn(cluster);
            })
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .doWithCluster(c -> c.register(unit.get(StateListener.class)))
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void doWithClusterBuilder() throws Exception {
    Cassandra.COUNTER.set(0);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind(null, Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(bind(null, Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(bind(null, MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(bind(null, Datastore.class))
            .expect(unit -> {
              Builder builder = unit.get(Cluster.Builder.class);
              expect(builder.withClusterName("mycluster")).andReturn(builder);
            })
            .expect(routeMapper).expect(onStop)
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .doWithClusterBuilder(b -> {
                    b.withClusterName("mycluster");
                  })
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  @Test
  public void secondInstance() throws Exception {
    Cassandra.COUNTER.set(1);
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
            .expect(clusterBuilder)
            .expect(contactPoints("localhost"))
            .expect(port(9042))
            .expect(codecRegistry)
            .expect(bind("beers", Cluster.class))
            .expect(bind("beers", Session.class))
            .expect(connect("beers"))
            .expect(mapper)
            .expect(bind("beers", MappingManager.class))
            .expect(datastore)
            .expect(bind("beers", Datastore.class))
            .expect(routeMapper).expect(onStop)
            .run(unit -> {
              new Cassandra("cassandra://localhost/beers")
                  .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
            });
  }

  private Block connect(final String keyspace) {
    return unit -> {
      Cluster cluster = unit.get(Cluster.class);
      expect(cluster.connect(keyspace)).andReturn(unit.get(Session.class));
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked" })
  private Block bind(final String name, final Class type) {
    return unit -> {
      Binder binder = unit.get(Binder.class);
      if (name == null) {
        AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
        aab.toInstance(unit.get(type));
        expect(binder.bind(Key.get(type))).andReturn(aab);
      } else {
        AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
        aab.toInstance(unit.get(type));
        expect(binder.bind(Key.get(type, Names.named(name)))).andReturn(aab);
      }
    };
  }

  private Block port(final int port) {
    return unit -> {
      Builder builder = unit.get(Cluster.Builder.class);
      expect(builder.withPort(port)).andReturn(builder);
    };
  }

  private Block contactPoints(final String... address) {
    return unit -> {
      Builder builder = unit.get(Cluster.Builder.class);
      expect(builder.addContactPoints(address)).andReturn(builder);
    };
  }
}
