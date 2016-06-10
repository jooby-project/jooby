package org.jooby.camel;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.isA;
import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import org.apache.camel.CamelContext;
import org.apache.camel.ConsumerTemplate;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.ShutdownRoute;
import org.apache.camel.ShutdownRunningTask;
import org.apache.camel.ThreadPoolRejectedPolicy;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.properties.PropertiesComponent;
import org.apache.camel.component.properties.PropertiesResolver;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.impl.DefaultShutdownStrategy;
import org.apache.camel.spi.ExecutorServiceManager;
import org.apache.camel.spi.ShutdownStrategy;
import org.apache.camel.spi.StreamCachingStrategy;
import org.apache.camel.spi.ThreadPoolProfile;
import org.jooby.Env;
import org.jooby.internal.camel.CamelFinalizer;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigObject;
import com.typesafe.config.ConfigOrigin;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({Camel.class, DefaultCamelContext.class, Multibinder.class })
public class CamelTest {

  private static int processors = Math.max(1, Runtime.getRuntime().availableProcessors());

  Config defConfig = ConfigFactory
      .parseResources(getClass(), "camel.conf")
      .withValue("runtime.processors-plus1", ConfigValueFactory.fromAnyRef(processors + 1))
      .withValue("runtime.processors-x2", ConfigValueFactory.fromAnyRef(processors * 2))
      .withValue("application.tmpdir",
          ConfigValueFactory.fromAnyRef(System.getProperty("java.io.tmpdir")))
      .withValue("file.separator",
          ConfigValueFactory.fromAnyRef(File.separator))
      .resolve()
      .getConfig("camel")
      .withValue("x", ConfigValueFactory.fromAnyRef("X"));

  private MockUnit.Block onStop = unit -> {
    Env env = unit.get(Env.class);
    expect(env.lifeCycle(CamelFinalizer.class)).andReturn(env);
  };

  @Test(expected = IllegalArgumentException.class)
  public void camelBadOption() throws Exception {
    Config camel = ConfigFactory.empty()
        .withValue("autoStartup", ConfigValueFactory.fromAnyRef("si"))
        .withValue("jmx", ConfigValueFactory.fromAnyRef("true"))
        .withValue("shutdown", ConfigValueFactory.fromAnyRef(ImmutableMap.builder().build()));
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);
        })
        .run(unit -> {
          new Camel()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(defConfig);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withStreamCaching() throws Exception {
    Config camel = defConfig
        .withValue("streamCaching.enabled", ConfigValueFactory.fromAnyRef(true));
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(
            unit -> {
              ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
              shutdownStrategy.setTimeout(10L);
              shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
              shutdownStrategy.setShutdownRoutesInReverseOrder(true);

              PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
                  new Class[]{String.class }, "application.conf");
              properties.setIgnoreMissingLocation(true);
              properties.setPropertiesResolver(isA(PropertiesResolver.class));
              properties.setPrefixToken("${");
              properties.setSuffixToken("}");

              ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
              threadProfile.setId("default");
              threadProfile.setDefaultProfile(true);
              threadProfile.setKeepAliveTime(60L);
              threadProfile.setMaxPoolSize(processors * 2);
              threadProfile.setPoolSize(processors + 1);
              threadProfile.setMaxQueueSize(1000);
              threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
              threadProfile.setTimeUnit(TimeUnit.SECONDS);

              ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
              executor.setDefaultThreadPoolProfile(threadProfile);

              StreamCachingStrategy streamCachingStrategy = unit.mock(StreamCachingStrategy.class);
              String dir = System.getProperty("java.io.tmpdir")
                  + File.separator + "camel" + File.separator + "#uuid#";
              streamCachingStrategy.setSpoolDirectory(new File(dir));
              expectLastCall().times(0, 1);
              streamCachingStrategy.setSpoolDirectory(dir);
              expectLastCall().times(0, 1);
              streamCachingStrategy.setEnabled(true);

              DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
              expect(ctx.getExecutorServiceManager()).andReturn(executor);
              expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
              expect(ctx.getStreamCachingStrategy()).andReturn(streamCachingStrategy);
              ctx.setAutoStartup(true);
              ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
              ctx.setHandleFault(false);
              ctx.setStreamCaching(true);
              ctx.setTracing(false);
              ctx.setAllowUseOriginalMessage(false);
              ctx.setShutdownRoute(ShutdownRoute.Default);
              ctx.disableJMX();
              ctx.addComponent("properties", properties);

              ProducerTemplate producer = unit.mock(ProducerTemplate.class);
              expect(ctx.createProducerTemplate()).andReturn(producer);

              AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              ptABB.toInstance(producer);

              ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
              expect(ctx.createConsumerTemplate()).andReturn(consumer);

              AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              ctABB.toInstance(consumer);

              AnnotatedBindingBuilder<CamelContext> ccABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              ccABB.toInstance(ctx);

              AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              dccABB.toInstance(ctx);

              AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              cfABB.asEagerSingleton();

              AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
                  .mock(AnnotatedBindingBuilder.class);
              rbABB.toInstance(isA(RouteBuilder.class));

              Binder binder = unit.get(Binder.class);
              expect(binder.bind(CamelContext.class)).andReturn(ccABB);
              expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
              expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
              expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
              expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
              expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

              Multibinder<Object> rbMB = unit.mock(Multibinder.class);

              unit.mockStatic(Multibinder.class);
              expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
                  .andReturn(rbMB);
            })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withJmx() throws Exception {
    Config camel = defConfig.withValue("jmx", ConfigValueFactory.fromAnyRef(true));

    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withConfigurer() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.addComponent("properties", properties);
          ctx.disableJMX();
          ctx.setDelayer(100L);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .doWith((ctx, config) -> {
                ctx.setDelayer(100L);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void withConfigurerErr() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          DefaultShutdownStrategy shutdownStrategy = unit
              .mockConstructor(DefaultShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.setShutdownStrategy(shutdownStrategy);
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .run(unit -> {
          new Camel()
              .doWith((ctx, config) -> {
                throw new IllegalStateException("intentional err");
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void withConfigurerCheckedErr() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          DefaultShutdownStrategy shutdownStrategy = unit
              .mockConstructor(DefaultShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.setShutdownStrategy(shutdownStrategy);
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .run(unit -> {
          new Camel()
              .doWith((ctx, config) -> {
                throw new IOException("intentional err");
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void withRoutes() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.addComponent("properties", properties);
          ctx.addRoutes(isA(RouteBuilder.class));

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .routes((router, config) -> {
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test(expected = IllegalStateException.class)
  public void withRoutesErr() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.addComponent("properties", properties);
          ctx.disableJMX();
          ctx.addRoutes(unit.capture(RouteBuilder.class));
          expectLastCall().andThrow(new Exception("intentional error"));

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .routes((router, config) -> {
              }).configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void propertyResolver() throws Exception {
    Config camel = defConfig;
    Config cprops = ConfigFactory.empty()
        .withValue("x", ConfigValueFactory.fromAnyRef("X"));
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);
          expect(config.entrySet()).andReturn(cprops.entrySet());

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(unit.capture(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, unit -> {
          Properties expected = new Properties();
          expected.setProperty("x", "X");
          PropertiesResolver resolver = unit.captured(PropertiesResolver.class).iterator().next();
          Properties props = resolver.resolveProperties(null, true, "application.conf");
          assertEquals(expected, props);
        });
  }

  @SuppressWarnings("unchecked")
  @Test
  public void routes() throws Exception {
    Config camel = defConfig;
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(unit -> {
          Config config = unit.get(Config.class);
          expect(config.getConfig("camel")).andReturn(camel);

          ConfigOrigin origin = unit.mock(ConfigOrigin.class);
          expect(origin.description()).andReturn("application.conf");

          ConfigObject root = unit.mock(ConfigObject.class);
          expect(root.origin()).andReturn(origin);

          expect(config.root()).andReturn(root);
        })
        .expect(unit -> {
          ShutdownStrategy shutdownStrategy = unit.mock(ShutdownStrategy.class);
          shutdownStrategy.setTimeout(10L);
          shutdownStrategy.setTimeUnit(TimeUnit.SECONDS);
          shutdownStrategy.setShutdownRoutesInReverseOrder(true);

          PropertiesComponent properties = unit.mockConstructor(PropertiesComponent.class,
              new Class[]{String.class }, "application.conf");
          properties.setIgnoreMissingLocation(true);
          properties.setPropertiesResolver(isA(PropertiesResolver.class));
          properties.setPrefixToken("${");
          properties.setSuffixToken("}");

          ThreadPoolProfile threadProfile = unit.mockConstructor(ThreadPoolProfile.class);
          threadProfile.setId("default");
          threadProfile.setDefaultProfile(true);
          threadProfile.setKeepAliveTime(60L);
          threadProfile.setMaxPoolSize(processors * 2);
          threadProfile.setPoolSize(processors + 1);
          threadProfile.setMaxQueueSize(1000);
          threadProfile.setRejectedPolicy(ThreadPoolRejectedPolicy.CallerRuns);
          threadProfile.setTimeUnit(TimeUnit.SECONDS);

          ExecutorServiceManager executor = unit.mock(ExecutorServiceManager.class);
          executor.setDefaultThreadPoolProfile(threadProfile);

          DefaultCamelContext ctx = unit.mockConstructor(DefaultCamelContext.class);
          expect(ctx.getExecutorServiceManager()).andReturn(executor);
          expect(ctx.getShutdownStrategy()).andReturn(shutdownStrategy);
          ctx.setAutoStartup(true);
          ctx.setShutdownRunningTask(ShutdownRunningTask.CompleteCurrentTaskOnly);
          ctx.setHandleFault(false);
          ctx.setStreamCaching(false);
          ctx.setTracing(false);
          ctx.setAllowUseOriginalMessage(false);
          ctx.setShutdownRoute(ShutdownRoute.Default);
          ctx.disableJMX();
          ctx.addComponent("properties", properties);

          ProducerTemplate producer = unit.mock(ProducerTemplate.class);
          expect(ctx.createProducerTemplate()).andReturn(producer);

          AnnotatedBindingBuilder<ProducerTemplate> ptABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ptABB.toInstance(producer);

          ConsumerTemplate consumer = unit.mock(ConsumerTemplate.class);
          expect(ctx.createConsumerTemplate()).andReturn(consumer);

          AnnotatedBindingBuilder<ConsumerTemplate> ctABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ctABB.toInstance(consumer);

          AnnotatedBindingBuilder<CamelContext> ccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          ccABB.toInstance(ctx);

          AnnotatedBindingBuilder<DefaultCamelContext> dccABB = unit
              .mock(AnnotatedBindingBuilder.class);
          dccABB.toInstance(ctx);

          AnnotatedBindingBuilder<CamelFinalizer> cfABB = unit
              .mock(AnnotatedBindingBuilder.class);
          cfABB.asEagerSingleton();

          AnnotatedBindingBuilder<RouteBuilder> rbABB = unit
              .mock(AnnotatedBindingBuilder.class);
          rbABB.toInstance(isA(RouteBuilder.class));

          Binder binder = unit.get(Binder.class);
          expect(binder.bind(CamelContext.class)).andReturn(ccABB);
          expect(binder.bind(DefaultCamelContext.class)).andReturn(dccABB);
          expect(binder.bind(ProducerTemplate.class)).andReturn(ptABB);
          expect(binder.bind(ConsumerTemplate.class)).andReturn(ctABB);
          expect(binder.bind(CamelFinalizer.class)).andReturn(cfABB);
          expect(binder.bind(RouteBuilder.class)).andReturn(rbABB);

          LinkedBindingBuilder<Object> rbMBLBB = unit.mock(LinkedBindingBuilder.class);
          expect(rbMBLBB.to(CamelTest.class)).andReturn(null);

          Multibinder<Object> rbMB = unit.mock(Multibinder.class);
          expect(rbMB.addBinding()).andReturn(rbMBLBB);

          unit.mockStatic(Multibinder.class);
          expect(Multibinder.newSetBinder(binder, Object.class, Names.named("camel.routes")))
              .andReturn(rbMB);
        })
        .expect(onStop)
        .run(unit -> {
          new Camel()
              .routes(CamelTest.class)
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        });
  }

  @Test
  public void config() {
    assertEquals(ConfigFactory.parseResources(getClass(), "camel.conf"), new Camel().config());
  }
}
