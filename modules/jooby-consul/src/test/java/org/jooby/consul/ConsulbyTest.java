package org.jooby.consul;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.name.Names;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.Route;
import org.jooby.Router;
import org.jooby.test.MockUnit;
import org.jooby.funzy.Throwing;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.concurrent.atomic.AtomicBoolean;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Consul.class, Consul.Builder.class})
public class ConsulbyTest {

  @Test
  public void defaults() throws Exception {
    Config config = resolvedConfig();
    mockUnit()
        .expect(envServiceKeyIsRequested())
        .expect(consulIsBuiltAndBound("default", config))
        .expect(serviceIsRegistered("default", config))
        .expect(healthCheckRouteIsBound("default", config))
        .run(createAndConfigureModule(config));
  }

  @Test
  public void defaultsWithExtraBuilder() throws Exception {
    Config config = resolvedConfig();
    mockUnit()
        .expect(envServiceKeyIsRequested())
        .expect(consulIsBuiltAndBound("default", config))
        .expect(serviceIsRegistered("default", config))
        .expect(healthCheckRouteIsBound("default", config))
        .run(createWithExtraBuilderAndConfigureModule(config));
  }

  @Test
  public void defaultsWithoutCheck() throws Exception {
    Config config = resolvedConfig()
        .withoutPath("consul.default.register.check");
    mockUnit()
        .expect(envServiceKeyIsRequested())
        .expect(consulIsBuiltAndBound("default", config))
        .expect(serviceIsRegistered("default", config))
        .run(createAndConfigureModule(config));
  }

  @Test
  public void defaultsWithoutRegister() throws Exception {
    Config config = resolvedConfig()
        .withoutPath("consul.default.register");
    mockUnit()
        .expect(envServiceKeyIsRequested())
        .expect(consulIsBuiltAndBound("default", config))
        .run(createAndConfigureModule(config));
  }

  @Test
  public void custom() throws Exception {
    Config config = resolvedConfig()
        .withValue("consul.custom.register.name", ConfigValueFactory.fromAnyRef("custom"));
    mockUnit()
        .expect(envServiceKeyIsRequested())
        .expect(consulIsBuiltAndBound("custom", config))
        .expect(serviceIsRegistered("custom", config))
        .expect(healthCheckRouteIsBound("custom", config))
        .run(createAndConfigureModule("custom", config));
  }

  @Test
  public void defaultConfig() throws Exception {
    assertEquals(consulConfig(), new Consulby().config());
  }

  private MockUnit mockUnit() {
    return new MockUnit(Env.class, Binder.class, Consul.class);
  }

  private MockUnit.Block envServiceKeyIsRequested() {
    return unit -> {
      Env env = unit.get(Env.class);
      expect(env.serviceKey()).andReturn(new Env.ServiceKey());
    };
  }

  private MockUnit.Block consulIsBuiltAndBound(String name, Config config) {
    return unit -> {

      Config consulConfig = consulConfigWithFallback(name, config);

      Consul consul = unit.get(Consul.class);

      Consul.Builder consulBuilder = unit.mock(Consul.Builder.class);
      expect(consulBuilder.withUrl(consulConfig.getString("url"))).andReturn(consulBuilder);
      expect(consulBuilder.build()).andReturn(consul);

      unit.mockStatic(Consul.class);
      expect(Consul.builder()).andReturn(consulBuilder);

      Env env = unit.get(Env.class);
      expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);

      //noinspection unchecked
      AnnotatedBindingBuilder<Consul> consulABB = unit.mock(AnnotatedBindingBuilder.class);
      consulABB.toInstance(consul);
      consulABB.toInstance(consul);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(Key.get(Consul.class))).andReturn(consulABB);
      expect(binder.bind(Key.get(Consul.class, Names.named(name)))).andReturn(consulABB);
    };
  }

  @SuppressWarnings("unused")
  private MockUnit.Block serviceIsRegistered(String name, Config config) {
    return unit -> {

      AgentClient agentClient = unit.mock(AgentClient.class);

      Consul consul = unit.get(Consul.class);
      expect(consul.agentClient()).andReturn(agentClient);

      Env env = unit.get(Env.class);
      expect(env.onStarted(isA(Throwing.Runnable.class))).andReturn(env);
      expect(env.onStop(isA(Throwing.Runnable.class))).andReturn(env);
    };
  }

  private MockUnit.Block healthCheckRouteIsBound(String name, Config config) {
    return unit -> {

      Config consulConfig = consulConfigWithFallback(name, config);

      Router router = unit.mock(Router.class);

      Env env = unit.get(Env.class);
      expect(env.router()).andReturn(router);

      String path = consulConfig.getString("register.check.path");
      Route.Definition definition = unit.mock(Route.Definition.class);
      expect(router.get(eq(path), isA(Route.ZeroArgHandler.class))).andReturn(definition);
    };
  }

  private MockUnit.Block createAndConfigureModule(Config config) {
    return unit -> new Consulby().configure(unit.get(Env.class), config, unit.get(Binder.class));
  }

  private MockUnit.Block createWithExtraBuilderAndConfigureModule(Config config) {
    return unit -> {
      AtomicBoolean consulBuilderConsumerCalled = new AtomicBoolean(false);
      AtomicBoolean registrationBuilderConsumerCalled = new AtomicBoolean(false);
      new Consulby()
          .withConsulBuilder(builder -> consulBuilderConsumerCalled.set(true))
          .withRegistrationBuilder(builder -> registrationBuilderConsumerCalled.set(true))
          .configure(unit.get(Env.class), config, unit.get(Binder.class));
      assertTrue("Consul Builder Consumer should be called", consulBuilderConsumerCalled.get());
      assertTrue("Registration Builder Consumer should be called",
          registrationBuilderConsumerCalled.get());
    };
  }

  @SuppressWarnings("SameParameterValue")
  private MockUnit.Block createAndConfigureModule(String name, Config config) {
    return unit -> new Consulby(name)
        .configure(unit.get(Env.class), config, unit.get(Binder.class));
  }

  private Config resolvedConfig() {
    return consulConfig()
        .withValue("application.name", ConfigValueFactory.fromAnyRef("testapp"))
        .withValue("application.host", ConfigValueFactory.fromAnyRef("localhost"))
        .withValue("application.port", ConfigValueFactory.fromAnyRef("8080"))
        .withValue("application.version", ConfigValueFactory.fromAnyRef("1.0.0-SNAPSHOT"))
        .resolve();
  }

  private Config consulConfigWithFallback(String name, Config config) {
    Config consulConfig = config.getConfig("consul.default");
    if (!name.equals("default") && config.hasPath("consul." + name)) {
      consulConfig = config.getConfig("consul." + name).withFallback(consulConfig);
    }
    return consulConfig;
  }

  private Config consulConfig() {
    return ConfigFactory.parseResources(getClass(), "consul.conf");
  }
}
