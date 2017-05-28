package org.jooby.neo4j;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static javaslang.control.Try.CheckedRunnable;
import static org.junit.Assert.assertEquals;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.graphaware.runtime.GraphAwareRuntime;
import com.graphaware.runtime.GraphAwareRuntimeFactory;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.factory.GraphDatabaseBuilder;
import org.neo4j.graphdb.factory.GraphDatabaseFactory;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;

@RunWith(PowerMockRunner.class)
@PrepareForTest({GraphDatabaseService.class, GraphAwareRuntime.class,
  GraphAwareRuntimeFactory.class, GraphDatabaseFactory.class,
  GraphDatabaseBuilder.class, EmbeddedNeo4j.class})
public class EmbeddedNeo4jTest {

  private Config $neo4j = ConfigFactory.parseResources(getClass(), "/embedded_neo4j.conf");
  private GraphDatabaseService dbService;
  private GraphAwareRuntime graphRuntime;

  @SuppressWarnings("unchecked")
  MockUnit.Block neo4j = unit -> {
    dbService = unit.mock(GraphDatabaseService.class);
    unit.registerMock(GraphDatabaseService.class, dbService);
    dbService.shutdown();
    expectLastCall();

    GraphDatabaseBuilder graphDatabaseBuilder = unit.mock(GraphDatabaseBuilder.class);
    unit.registerMock(GraphDatabaseBuilder.class, graphDatabaseBuilder);
    expect(graphDatabaseBuilder.newGraphDatabase()).andReturn(dbService);

    GraphDatabaseFactory graphDatabaseFactory = unit.mockConstructor(GraphDatabaseFactory.class);
    expect(graphDatabaseFactory.newEmbeddedDatabaseBuilder(isA(File.class))).andReturn(graphDatabaseBuilder);
    unit.registerMock(GraphDatabaseFactory.class, graphDatabaseFactory);

    graphRuntime = unit.mock(GraphAwareRuntime.class);
    unit.registerMock(GraphAwareRuntime.class, graphRuntime);

    unit.mockStatic(GraphAwareRuntimeFactory.class);
    expect(GraphAwareRuntimeFactory.createRuntime(dbService)).andReturn(graphRuntime);

    AnnotatedBindingBuilder<GraphDatabaseService> dbsABB = unit.mock(AnnotatedBindingBuilder.class);
    dbsABB.toInstance(dbService);
    dbsABB.toInstance(dbService);

    AnnotatedBindingBuilder<GraphAwareRuntime> grABB = unit.mock(AnnotatedBindingBuilder.class);
    grABB.toInstance(graphRuntime);
    grABB.toInstance(graphRuntime);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(Key.get(GraphDatabaseService.class))).andReturn(dbsABB);
    expect(binder.bind(Key.get(GraphDatabaseService.class, Names.named("/tmp")))).andReturn(dbsABB);

    expect(binder.bind(Key.get(GraphAwareRuntime.class))).andReturn(grABB);
    expect(binder.bind(Key.get(GraphAwareRuntime.class, Names.named("/tmp")))).andReturn(grABB);

    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(CheckedRunnable.class))).andReturn(env);
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Config.class, Binder.class, Env.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j")).andReturn(false);
      })
      .expect(serviceKey(new ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        expect(dbService.isAvailable(60000L)).andReturn(true);
      })
      .run(unit -> {
        EmbeddedNeo4j embeddedNeo4j = new EmbeddedNeo4j();
        embeddedNeo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
    }, unit -> {
        unit.captured(CheckedRunnable.class).iterator().next().run();
    });
  }

  @Test(expected = IllegalArgumentException.class)
  public void shouldFailWhenDbIsMissing() throws Exception {
    new MockUnit(Config.class, Binder.class, Env.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j")).andReturn(false);
      })
      .expect(neo4j)
      .expect(unit -> {
        expect(dbService.isAvailable(60000L)).andReturn(false);
      })
      .run(unit -> {
        EmbeddedNeo4j embeddedNeo4j = new EmbeddedNeo4j();
        embeddedNeo4j.configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      });
  }

  @Test
  public void defaultsWithCustomAction() throws Exception {
    new MockUnit(Config.class, Binder.class, Env.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j")).andReturn(true);
        expect(config.getConfig("neo4j")).andReturn(ConfigFactory.empty()
          .withValue("databaseDir", ConfigValueFactory.fromAnyRef("/tmp")));
      })
      .expect(serviceKey(new ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        expect(dbService.isAvailable(60000L)).andReturn(true);
      })
      .run(unit -> {
        new EmbeddedNeo4j()
          .properties((properties, config) -> {
            assertEquals("/tmp", properties.getProperty("databaseDir"));
          })
          .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      }, unit -> {
        unit.captured(CheckedRunnable.class).iterator().next().run();
      });
  }

  @Test
  public void defaultsConfig() throws Exception {
    new MockUnit(Config.class, Binder.class, Env.class)
      .expect(unit -> {
        assertEquals($neo4j, new EmbeddedNeo4j().config());
      });
  }

  @Test
  public void defaultsWithProperties() throws Exception {
    new MockUnit(Config.class, Binder.class, Env.class)
      .expect(unit -> {
        Config config = unit.get(Config.class);
        expect(config.getConfig("neo4j")).andReturn($neo4j.getConfig("neo4j"));
        expect(config.hasPath("neo4j")).andReturn(false);
      })
      .expect(serviceKey(new ServiceKey()))
      .expect(neo4j)
      .expect(unit -> {
        expect(dbService.isAvailable(60000L)).andReturn(true); 
      })
      .run(unit -> {
        new EmbeddedNeo4j()
          .properties((properties, config) -> {
            properties.put("databaseDir", "/tmp");
          })
          .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
      }, unit -> {
         unit.captured(CheckedRunnable.class).iterator().next().run();
      });
  }

  private Block serviceKey(final ServiceKey serviceKey) {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(serviceKey);
    };
  }
}