package org.jooby.cassandra;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.Cluster.Builder;
import com.datastax.driver.core.CodecRegistry;
import com.datastax.driver.core.Configuration;
import com.datastax.driver.core.DelegatingCluster;
import com.datastax.driver.core.Host.StateListener;
import com.datastax.driver.core.Session;
import com.datastax.driver.dse.DseCluster;
import com.datastax.driver.dse.DseSession;
import com.datastax.driver.extras.codecs.jdk8.InstantCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalDateCodec;
import com.datastax.driver.extras.codecs.jdk8.LocalTimeCodec;
import com.datastax.driver.mapping.MappingManager;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Router;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.HashSet;
import java.util.Set;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Cassandra.class, Cluster.class, Cluster.Builder.class, CodecRegistry.class,
    MappingManager.class, Datastore.class, CassandraMapper.class})
public class CassandraTest {

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Block clusterBuilder = unit -> {
    String cname = Cluster.class.getName();
    unit.mockStatic(Cluster.class);
    expect(Cluster.class.getName()).andReturn(cname);
    expect(Cluster.class.getInterfaces()).andReturn(new Class[0]);
    expect(Cluster.class.isInterface()).andReturn(false);
    Class obj = Object.class;
    expect(Cluster.class.getSuperclass()).andReturn(obj);

    Cluster cluster = unit.get(Cluster.class);
    expect(cluster.getConfiguration()).andReturn(unit.get(Configuration.class));

    Builder builder = unit.get(Cluster.Builder.class);
    expect(Cluster.builder()).andReturn(builder);
    expect(builder.build()).andReturn(cluster);
  };

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Block clusterBuilderProvider = unit -> {
    String cname = Cluster.class.getName();
    unit.mockStatic(Cluster.class);
    expect(Cluster.class.getName()).andReturn(cname);
    expect(Cluster.class.getInterfaces()).andReturn(new Class[0]);
    expect(Cluster.class.isInterface()).andReturn(false);
    Class obj = Object.class;
    expect(Cluster.class.getSuperclass()).andReturn(obj);

    Cluster cluster = unit.get(Cluster.class);
    expect(cluster.getConfiguration()).andReturn(unit.get(Configuration.class));

    Builder builder = unit.get(Cluster.Builder.class);
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
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  private Block routeMapper = unit -> {
    CassandraMapper mapper = unit.constructor(CassandraMapper.class)
        .build();

    Router routes = unit.mock(Router.class);
    expect(routes.map(mapper)).andReturn(routes);

    Env env = unit.get(Env.class);
    expect(env.router()).andReturn(routes);
  };

  @Test
  public void connectViaProperty() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.getString("db")).andReturn("cassandra://localhost/beers");
        })
        .expect(serviceKey(new Env.ServiceKey()))
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
  public void connectViaPropertySupplier() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.getString("db")).andReturn("cassandra://localhost/beers");
        })
        .expect(serviceKey(new Env.ServiceKey()))
        .expect(clusterBuilderProvider)
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
          new Cassandra(() -> unit.get(Cluster.Builder.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  private Block serviceKey(final ServiceKey serviceKey) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(serviceKey);
    };
  }

  @Test
  public void connectViaConnectionString() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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
  public void connectViaConnectionStringSupplier() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilderProvider)
        .expect(serviceKey(new Env.ServiceKey()))
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
          new Cassandra("cassandra://localhost/beers", () -> unit.get(Cluster.Builder.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void onStop() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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
          unit.captured(Throwing.Runnable.class).iterator().next().run();
        });
  }

  @Test
  public void onStopSessionerr() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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
          unit.captured(Throwing.Runnable.class).iterator().next().run();
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withAccessor() throws Exception {
    Object value = new Object();
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class, StateListener.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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
    new MockUnit(Env.class, Config.class, Binder.class, Cluster.class, Cluster.Builder.class,
        Configuration.class, Session.class)
        .expect(clusterBuilder)
        .expect(serviceKey(new Env.ServiceKey()))
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

  private Block connect(final String keyspace) {
    return unit -> {
      Cluster cluster = unit.get(Cluster.class);
      expect(cluster.connect(keyspace)).andReturn(unit.get(Session.class));
    };
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private Block bind(final String name, final Class type) {
    return unit -> {
      Binder binder = unit.get(Binder.class);
      if (name == null) {
        AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
        Object val = unit.get(type);
        aab.toInstance(val);
        expectLastCall().times(1, 2);
        expect(binder.bind(Key.get(type))).andReturn(aab);
        expect(binder.bind(Key.get(val.getClass()))).andReturn(aab).times(0, 1);
      } else {
        AnnotatedBindingBuilder aab = unit.mock(AnnotatedBindingBuilder.class);
        Object val = unit.get(type);
        aab.toInstance(val);
        expectLastCall().times(1, 2);
        expect(binder.bind(Key.get(type, Names.named(name)))).andReturn(aab);
        expect(binder.bind(Key.get(val.getClass(), Names.named(name)))).andReturn(aab).times(0, 1);
      }
    };
  }

  @Test
  public void hierarchy() {
    assertEquals(ImmutableSet.of(), hierarchy(Object.class));
    // normal
    assertEquals(ImmutableSet.of(Cluster.class), hierarchy(Cluster.class));
    assertEquals(ImmutableSet.of(Session.class), hierarchy(Session.class));
    // dse
    assertEquals(ImmutableSet.of(DseCluster.class, DelegatingCluster.class, Cluster.class),
        hierarchy(DseCluster.class));
    assertEquals(ImmutableSet.of(DseSession.class, Session.class), hierarchy(DseSession.class));
  }

  @SuppressWarnings("rawtypes")
  private Set<Class> hierarchy(final Class type) {
    Set<Class> types = new HashSet<>();
    Cassandra.hierarchy(type, types::add);
    return types;
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
