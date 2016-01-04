package org.jooby.hbm;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Provider;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.sql.DataSource;

import org.jooby.Env;
import org.jooby.Route;
import org.jooby.Route.Definition;
import org.jooby.internal.hbm.HbmProvider;
import org.jooby.internal.hbm.HbmUnitDescriptor;
import org.jooby.jdbc.Jdbc;
import org.jooby.scope.RequestScoped;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.OutOfScopeException;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Hbm.class, HbmProvider.class, HbmUnitDescriptor.class, Multibinder.class,
    Route.Definition.class })
public class HbmTest {

  Config config = ConfigFactory.parseResources(Hbm.class, "hbm.conf")
      .withFallback(ConfigFactory.parseResources(Jdbc.class, "jdbc.conf"))
      .withValue("db", ConfigValueFactory.fromAnyRef("mem"))
      .withValue("application.ns", ConfigValueFactory.fromAnyRef("x.y.z"));

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);
          scope.asEagerSingleton();
          scope.asEagerSingleton();

          LinkedBindingBuilder<DataSource> binding = unit.mock(LinkedBindingBuilder.class);
          expect(binding.toProvider(isA(Provider.class))).andReturn(scope).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
          expect(binder.bind(Key.get(DataSource.class, Names.named("db")))).andReturn(binding);
        })
        .expect(unit -> {
          ScopedBindingBuilder emSBB = unit.mock(ScopedBindingBuilder.class);
          emSBB.in(RequestScoped.class);
          emSBB.in(RequestScoped.class);

          LinkedBindingBuilder<EntityManager> emLBB = unit.mock(LinkedBindingBuilder.class);
          expect(emLBB.toProvider(isA(Provider.class))).andReturn(emSBB).times(2);

          ScopedBindingBuilder hpSBB = unit.mock(ScopedBindingBuilder.class);
          hpSBB.asEagerSingleton();
          hpSBB.asEagerSingleton();

          unit.mockConstructor(HbmUnitDescriptor.class,
              new Class[]{ClassLoader.class, Provider.class, Config.class, Set.class },
              eq(Hbm.class.getClassLoader()), isA(Provider.class), eq(config),
              eq(Collections.emptySet()));

          LinkedBindingBuilder<EntityManagerFactory> emfLBB = unit
              .mock(LinkedBindingBuilder.class);
          expect(emfLBB.toProvider(isA(HbmProvider.class))).andReturn(hpSBB).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(EntityManagerFactory.class))).andReturn(emfLBB);
          expect(binder.bind(Key.get(EntityManagerFactory.class, Names.named("db"))))
              .andReturn(emfLBB);

          expect(binder.bind(Key.get(EntityManager.class))).andReturn(emLBB);
          expect(binder.bind(Key.get(EntityManager.class, Names.named("db")))).andReturn(emLBB);

          OpenSessionInView osiv = unit
              .mockConstructor(OpenSessionInView.class,
                  new Class[]{Provider.class, List.class }, isA(HbmProvider.class),
                  isA(List.class));

          Route.Definition route = unit.mockConstructor(
              Route.Definition.class,
              new Class[]{String.class, String.class, Route.Filter.class },
              "*", "*", osiv);
          expect(route.name("hbm")).andReturn(route);

          LinkedBindingBuilder<Definition> rdLBB = unit.mock(LinkedBindingBuilder.class);
          rdLBB.toInstance(route);

          Multibinder<Definition> mbrd = unit.mock(Multibinder.class);
          expect(mbrd.addBinding()).andReturn(rdLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(mbrd);
        })
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = OutOfScopeException.class)
  public void outOfScope() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);
          scope.asEagerSingleton();
          scope.asEagerSingleton();

          LinkedBindingBuilder<DataSource> binding = unit.mock(LinkedBindingBuilder.class);
          expect(binding.toProvider(isA(Provider.class))).andReturn(scope).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
          expect(binder.bind(Key.get(DataSource.class, Names.named("db")))).andReturn(binding);
        })
        .expect(unit -> {
          ScopedBindingBuilder emSBB = unit.mock(ScopedBindingBuilder.class);
          emSBB.in(RequestScoped.class);
          emSBB.in(RequestScoped.class);

          LinkedBindingBuilder<EntityManager> emLBB = unit.mock(LinkedBindingBuilder.class);
          expect(emLBB.toProvider(unit.capture(Provider.class))).andReturn(emSBB).times(2);

          ScopedBindingBuilder hpSBB = unit.mock(ScopedBindingBuilder.class);
          hpSBB.asEagerSingleton();
          hpSBB.asEagerSingleton();

          LinkedBindingBuilder<EntityManagerFactory> emfLBB = unit
              .mock(LinkedBindingBuilder.class);
          expect(emfLBB.toProvider(isA(HbmProvider.class))).andReturn(hpSBB).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(EntityManagerFactory.class))).andReturn(emfLBB);
          expect(binder.bind(Key.get(EntityManagerFactory.class, Names.named("db"))))
              .andReturn(emfLBB);

          expect(binder.bind(Key.get(EntityManager.class))).andReturn(emLBB);
          expect(binder.bind(Key.get(EntityManager.class, Names.named("db")))).andReturn(emLBB);

          OpenSessionInView osiv = unit
              .mockConstructor(OpenSessionInView.class,
                  new Class[]{Provider.class, List.class }, isA(HbmProvider.class),
                  isA(List.class));

          Route.Definition route = unit.mockConstructor(
              Route.Definition.class,
              new Class[]{String.class, String.class, Route.Filter.class },
              "*", "*", osiv);
          expect(route.name("hbm")).andReturn(route);

          LinkedBindingBuilder<Definition> rdLBB = unit.mock(LinkedBindingBuilder.class);
          rdLBB.toInstance(route);

          Multibinder<Definition> mbrd = unit.mock(Multibinder.class);
          expect(mbrd.addBinding()).andReturn(rdLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(mbrd);
        })
        .run(unit -> {
          new Hbm().configure(unit.get(Env.class), config, unit.get(Binder.class));
        } , unit -> {
          unit.captured(Provider.class).forEach(Provider::get);
        });
  }

  @Test
  public void config() {
    assertEquals(ConfigFactory.parseResources(Hbm.class, "hbm.conf")
        .withFallback(ConfigFactory.parseResources(Jdbc.class, "jdbc.conf")),
        new Hbm().config());
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaultsScan() throws Exception {
    new MockUnit(Env.class, Binder.class)
        .expect(unit -> {
          Env env = unit.get(Env.class);
          expect(env.name()).andReturn("dev");
        })
        .expect(unit -> {
          ScopedBindingBuilder scope = createMock(ScopedBindingBuilder.class);
          scope.asEagerSingleton();
          scope.asEagerSingleton();

          LinkedBindingBuilder<DataSource> binding = unit.mock(LinkedBindingBuilder.class);
          expect(binding.toProvider(isA(Provider.class))).andReturn(scope).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(DataSource.class))).andReturn(binding);
          expect(binder.bind(Key.get(DataSource.class, Names.named("db")))).andReturn(binding);
        })
        .expect(unit -> {
          ScopedBindingBuilder emSBB = unit.mock(ScopedBindingBuilder.class);
          emSBB.in(RequestScoped.class);
          emSBB.in(RequestScoped.class);

          LinkedBindingBuilder<EntityManager> emLBB = unit.mock(LinkedBindingBuilder.class);
          expect(emLBB.toProvider(isA(Provider.class))).andReturn(emSBB).times(2);

          ScopedBindingBuilder hpSBB = unit.mock(ScopedBindingBuilder.class);
          hpSBB.asEagerSingleton();
          hpSBB.asEagerSingleton();

          unit.mockConstructor(HbmUnitDescriptor.class,
              new Class[]{ClassLoader.class, Provider.class, Config.class, Set.class },
              eq(Hbm.class.getClassLoader()), isA(Provider.class), eq(config),
              eq(Sets.newHashSet("x.y.z")));

          LinkedBindingBuilder<EntityManagerFactory> emfLBB = unit
              .mock(LinkedBindingBuilder.class);
          expect(emfLBB.toProvider(isA(HbmProvider.class))).andReturn(hpSBB).times(2);

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(Key.get(EntityManagerFactory.class))).andReturn(emfLBB);
          expect(binder.bind(Key.get(EntityManagerFactory.class, Names.named("db"))))
              .andReturn(emfLBB);

          expect(binder.bind(Key.get(EntityManager.class))).andReturn(emLBB);
          expect(binder.bind(Key.get(EntityManager.class, Names.named("db")))).andReturn(emLBB);

          OpenSessionInView osiv = unit
              .mockConstructor(OpenSessionInView.class,
                  new Class[]{Provider.class, List.class }, isA(HbmProvider.class),
                  isA(List.class));

          Route.Definition route = unit.mockConstructor(
              Route.Definition.class,
              new Class[]{String.class, String.class, Route.Filter.class },
              "*", "*", osiv);
          expect(route.name("hbm")).andReturn(route);

          LinkedBindingBuilder<Definition> rdLBB = unit.mock(LinkedBindingBuilder.class);
          rdLBB.toInstance(route);

          Multibinder<Definition> mbrd = unit.mock(Multibinder.class);
          expect(mbrd.addBinding()).andReturn(rdLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Route.Definition.class)).andReturn(mbrd);
        })
        .run(unit -> {
          new Hbm().scan().configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }
}
