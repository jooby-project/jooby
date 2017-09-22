package org.jooby.requery;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.util.Types;
import com.typesafe.config.Config;
import io.requery.EntityStore;
import io.requery.Persistable;
import io.requery.TransactionIsolation;
import io.requery.TransactionListener;
import io.requery.async.CompletableEntityStore;
import io.requery.async.CompletionStageEntityStore;
import io.requery.meta.EntityModel;
import io.requery.meta.Type;
import io.requery.reactivex.ReactiveEntityStore;
import io.requery.reactivex.ReactiveSupport;
import io.requery.reactor.ReactorEntityStore;
import io.requery.sql.BoundParameters;
import io.requery.sql.Configuration;
import io.requery.sql.ConfigurationBuilder;
import io.requery.sql.EntityDataStore;
import io.requery.sql.EntityStateListener;
import io.requery.sql.SchemaModifier;
import io.requery.sql.StatementListener;
import io.requery.sql.TableCreationMode;
import io.requery.util.function.Supplier;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Env.ServiceKey;
import org.jooby.Registry;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.inject.Provider;
import javax.sql.CommonDataSource;
import javax.sql.DataSource;
import java.sql.Statement;
import java.util.Set;
import java.util.function.Consumer;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Requery.class, ConfigurationBuilder.class, EntityDataStore.class,
    SchemaModifier.class, ReactiveSupport.class})
public class RequeryTest {

  public static class Person implements Persistable {
  }

  private Block keys = unit -> {
    ServiceKey keys = unit.mock(Env.ServiceKey.class);
    unit.registerMock(Env.ServiceKey.class, keys);

    Env env = unit.get(Env.class);
    expect(env.serviceKey()).andReturn(keys);
  };

  private Block zeroModels = unit -> {
    EntityModel model = unit.get(EntityModel.class);
    expect(model.getName()).andReturn("DEFAULT");
    expect(model.getTypes()).andReturn(ImmutableSet.of());
  };

  @SuppressWarnings("unchecked")
  private Block onStart = unit -> {
    Env env = unit.get(Env.class);
    expect(env.onStart(unit.capture(Throwing.Consumer.class))).andReturn(null);
  };

  private Block configurationBuilder = unit -> {
    Configuration conf = unit.mock(Configuration.class);
    unit.registerMock(Configuration.class, conf);

    ConfigurationBuilder cb = unit.constructor(ConfigurationBuilder.class)
        .args(CommonDataSource.class, EntityModel.class)
        .build(unit.get(DataSource.class), unit.get(EntityModel.class));

    unit.registerMock(ConfigurationBuilder.class, cb);
    expect(cb.build()).andReturn(conf);
  };

  private Block eds = unit -> {
    Configuration conf = unit.get(Configuration.class);
    @SuppressWarnings("rawtypes")
    EntityDataStore eds = unit.constructor(EntityDataStore.class)
        .args(Configuration.class)
        .build(conf);
    unit.registerMock(EntityDataStore.class, eds);
  };

  private Block noSchema = unit -> {
    Config conf = unit.get(Config.class);
    expect(conf.hasPath("requery.schema")).andReturn(false);
  };

  @Test
  public void newModule() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class)
        .expect(keys)
        .expect(zeroModels)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldBindDataSource() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldBindDataSouxrce() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldBindCustomDataSouxrce() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(configurationBuilder)
        .expect(eds)
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .dataSource(() -> unit.get(DataSource.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void reactiveStore() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(ReactiveEntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          unit.mockStatic(ReactiveSupport.class);
          expect(ReactiveSupport.toReactiveStore(unit.get(EntityDataStore.class)))
              .andReturn(null);
        })
        .run(unit -> {
          Requery.reactive(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void reactorStore() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(ReactorEntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          unit.constructor(ReactorEntityStore.class)
              .build(unit.get(EntityDataStore.class));
        })
        .run(unit -> {
          Requery.reactor(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void completableEntityStore() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(CompletionStageEntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          unit.constructor(CompletableEntityStore.class)
              .build(unit.get(EntityDataStore.class));
        })
        .run(unit -> {
          Requery.completionStage(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldInvokeConfigurerCallback() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          ConfigurationBuilder builder = unit.get(ConfigurationBuilder.class);
          expect(builder.useDefaultLogging()).andReturn(builder);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .doWith(builder -> {
                builder.useDefaultLogging();
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldSetupEntityStateListener() throws Exception {
    class MyListener implements EntityStateListener<Object> {
      @Override
      public void postLoad(final Object entity) {
      }

      @Override
      public void postInsert(final Object entity) {
      }

      @Override
      public void postDelete(final Object entity) {
      }

      @Override
      public void postUpdate(final Object entity) {
      }

      @Override
      public void preInsert(final Object entity) {
      }

      @Override
      public void preDelete(final Object entity) {
      }

      @Override
      public void preUpdate(final Object entity) {
      }

    }
    MyListener listener = new MyListener();
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          ConfigurationBuilder builder = unit.get(ConfigurationBuilder.class);
          expect(builder.addEntityStateListener(listener)).andReturn(builder);

          Registry registry = unit.get(Registry.class);
          expect(registry.require(MyListener.class)).andReturn(listener);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .entityStateListener(MyListener.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldSetupStatementListener() throws Exception {
    class MyListener implements StatementListener {

      @Override
      public void beforeExecuteUpdate(final Statement statement, final String sql,
          final BoundParameters parameters) {
      }

      @Override
      public void afterExecuteUpdate(final Statement statement, final int count) {
      }

      @Override
      public void beforeExecuteBatchUpdate(final Statement statement, final String sql) {
      }

      @Override
      public void afterExecuteBatchUpdate(final Statement statement, final int[] count) {
      }

      @Override
      public void beforeExecuteQuery(final Statement statement, final String sql,
          final BoundParameters parameters) {
      }

      @Override
      public void afterExecuteQuery(final Statement statement) {
      }
    }
    MyListener listener = new MyListener();
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          ConfigurationBuilder builder = unit.get(ConfigurationBuilder.class);
          expect(builder.addStatementListener(listener)).andReturn(builder);

          Registry registry = unit.get(Registry.class);
          expect(registry.require(MyListener.class)).andReturn(listener);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .statementListener(MyListener.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldSetupTransactionLister() throws Exception {
    class MyListener implements TransactionListener {

      @Override
      public void beforeBegin(final TransactionIsolation isolation) {
      }

      @Override
      public void afterBegin(final TransactionIsolation isolation) {
      }

      @Override
      public void beforeCommit(final Set<Type<?>> types) {
      }

      @Override
      public void afterCommit(final Set<Type<?>> types) {
      }

      @Override
      public void beforeRollback(final Set<Type<?>> types) {
      }

      @Override
      public void afterRollback(final Set<Type<?>> types) {
      }
    }
    MyListener listener = new MyListener();
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          ConfigurationBuilder builder = unit.get(ConfigurationBuilder.class);
          expect(builder.addTransactionListenerFactory(unit.capture(Supplier.class)))
              .andReturn(builder);

          Registry registry = unit.get(Registry.class);
          expect(registry.require(MyListener.class)).andReturn(listener);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .transactionListener(MyListener.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
          assertEquals(listener, unit.captured(Supplier.class).iterator().next().get());
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldCreateSchema() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels).expect(noSchema)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          SchemaModifier schema = unit.constructor(SchemaModifier.class)
              .args(DataSource.class, EntityModel.class)
              .build(unit.get(DataSource.class), unit.get(EntityModel.class));
          schema.createTables(TableCreationMode.CREATE_NOT_EXISTS);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .schema(TableCreationMode.CREATE_NOT_EXISTS)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void shouldCreateSchemaFromProperty() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class, Registry.class,
        DataSource.class)
        .expect(keys)
        .expect(zeroModels)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(dataSource(Key.get(DataSource.class)))
        .expect(configurationBuilder)
        .expect(eds)
        .expect(unit -> {
          Config conf = unit.get(Config.class);
          expect(conf.hasPath("requery.schema")).andReturn(true);
          expect(conf.getString("requery.schema")).andReturn("DROP_CREATE");
        })
        .expect(unit -> {
          SchemaModifier schema = unit.constructor(SchemaModifier.class)
              .args(DataSource.class, EntityModel.class)
              .build(unit.get(DataSource.class), unit.get(EntityModel.class));
          schema.createTables(TableCreationMode.DROP_CREATE);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Throwing.Consumer.class).iterator().next()
              .accept(unit.get(Registry.class));
        });
  }

  @Test
  @SuppressWarnings({"rawtypes", "unchecked"})
  public void newModuleWithTypes() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class)
        .expect(keys)
        .expect(unit -> {
          Type type = unit.mock(Type.class);
          expect(type.getClassType()).andReturn(Person.class);

          LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
          expect(lbb.toProvider(isA(Provider.class))).andReturn(lbb);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key
              .get(Types.newParameterizedType(EntityStore.class, Persistable.class, Person.class))))
              .andReturn(lbb);

          EntityModel model = unit.get(EntityModel.class);
          expect(model.getName()).andReturn("foo");
          expect(model.getTypes()).andReturn(ImmutableSet.of(type));
        })
        .expect(store(EntityStore.class, "foo"))
        .expect(onStart)
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  @Test
  public void bindEntityStore() throws Exception {
    Key k = Key.get(Object.class);
    new MockUnit(Env.class, Config.class, Binder.class, EntityModel.class)
        .expect(keys)
        .expect(zeroModels)
        .expect(store(EntityStore.class, "DEFAULT"))
        .expect(onStart)
        .expect(unit -> {
          LinkedBindingBuilder lbb = unit.mock(LinkedBindingBuilder.class);
          expect(lbb.toProvider(isA(Provider.class))).andReturn(lbb);
          Binder binder = unit.get(Binder.class);
          expect(binder.bind(k)).andReturn(lbb);
        })
        .run(unit -> {
          new Requery(unit.get(EntityModel.class))
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          unit.captured(Consumer.class).iterator().next().accept(k);
        });
  }

  @SuppressWarnings({"unchecked", "rawtypes"})
  private Block store(final Class<? extends EntityStore> storeType, final String db) {
    return unit -> {
      Env.ServiceKey keys = unit.get(Env.ServiceKey.class);
      keys.generate(eq(storeType), eq(db), unit.capture(Consumer.class));
    };
  }

  private Block dataSource(final Key<DataSource> dbkey) {
    return unit -> {
      DataSource ds = unit.get(DataSource.class);

      Registry registry = unit.get(Registry.class);
      expect(registry.require(DataSource.class)).andReturn(ds);
    };
  }
}
