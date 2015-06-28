package org.jooby.memcached;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;

import java.util.Arrays;

import net.spy.memcached.ConnectionFactoryBuilder;
import net.spy.memcached.ConnectionFactoryBuilder.Locator;
import net.spy.memcached.ConnectionFactoryBuilder.Protocol;
import net.spy.memcached.FailureMode;
import net.spy.memcached.MemcachedClient;
import net.spy.memcached.auth.AuthDescriptor;
import net.spy.memcached.metrics.MetricType;

import org.jooby.Env;
import org.jooby.internal.memcached.MemcachedClientProvider;
import org.jooby.test.MockUnit;
import org.jooby.test.MockUnit.Block;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.ScopedBindingBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigValueFactory;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SpyMemcached.class, ConnectionFactoryBuilder.class })
public class SpyMemcachedTest {

  private Block cfb = unit -> {
    ConnectionFactoryBuilder cfb = unit.mockConstructor(ConnectionFactoryBuilder.class);
    unit.registerMock(ConnectionFactoryBuilder.class, cfb);
  };

  private Block defprops = unit -> {
    ConnectionFactoryBuilder cfb = unit.get(ConnectionFactoryBuilder.class);
    expect(cfb.setAuthWaitTime(1000L)).andReturn(cfb);
    expect(cfb.setDaemon(false)).andReturn(cfb);
    expect(cfb.setFailureMode(FailureMode.Redistribute)).andReturn(cfb);
    expect(cfb.setLocatorType(Locator.ARRAY_MOD)).andReturn(cfb);
    expect(cfb.setMaxReconnectDelay(30)).andReturn(cfb);
    expect(cfb.setProtocol(Protocol.TEXT)).andReturn(cfb);
  };

  private Block fullprops = unit -> {
    ConnectionFactoryBuilder cfb = unit.get(ConnectionFactoryBuilder.class);
    expect(cfb.setAuthWaitTime(1000L)).andReturn(cfb);
    expect(cfb.setDaemon(false)).andReturn(cfb);
    expect(cfb.setFailureMode(FailureMode.Redistribute)).andReturn(cfb);
    expect(cfb.setLocatorType(Locator.ARRAY_MOD)).andReturn(cfb);
    expect(cfb.setMaxReconnectDelay(30)).andReturn(cfb);
    expect(cfb.setProtocol(Protocol.TEXT)).andReturn(cfb);

    expect(cfb.setEnableMetrics(MetricType.OFF)).andReturn(cfb);
    expect(cfb.setOpQueueMaxBlockTime(1000L)).andReturn(cfb);
    expect(cfb.setOpTimeout(1000L)).andReturn(cfb);
    expect(cfb.setReadBufferSize(100)).andReturn(cfb);
    expect(cfb.setShouldOptimize(true)).andReturn(cfb);
    expect(cfb.setTimeoutExceptionThreshold(10)).andReturn(cfb);
    expect(cfb.setUseNagleAlgorithm(true)).andReturn(cfb);
  };

  @SuppressWarnings("unchecked")
  private Block bind = unit -> {
    ScopedBindingBuilder sbbMC = unit.mock(ScopedBindingBuilder.class);
    sbbMC.asEagerSingleton();

    AnnotatedBindingBuilder<MemcachedClient> abbMC = unit.mock(AnnotatedBindingBuilder.class);
    expect(abbMC.toProvider(isA(MemcachedClientProvider.class))).andReturn(sbbMC);

    Binder binder = unit.get(Binder.class);
    expect(binder.bind(MemcachedClient.class)).andReturn(abbMC);
  };

  @Test
  public void configure() throws Exception {
    Config config = new SpyMemcached().config()
        .withValue("memcached.server", ConfigValueFactory.fromAnyRef("localhost:11211"));
    new MockUnit(Env.class, Binder.class)
        .expect(cfb)
        .expect(defprops)
        .expect(bind)
        .run(unit -> {
          new SpyMemcached()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @Test
  public void fullprops() throws Exception {
    Config config = new SpyMemcached().config()
        .withValue("memcached.server", ConfigValueFactory.fromAnyRef("localhost:11211"))
        .withValue("memcached.enableMetrics", ConfigValueFactory.fromAnyRef("off"))
        .withValue("memcached.opQueueMaxBlockTime", ConfigValueFactory.fromAnyRef("1s"))
        .withValue("memcached.opTimeout", ConfigValueFactory.fromAnyRef("1s"))
        .withValue("memcached.readBufferSize", ConfigValueFactory.fromAnyRef(100))
        .withValue("memcached.shouldOptimize", ConfigValueFactory.fromAnyRef(true))
        .withValue("memcached.timeoutExceptionThreshold", ConfigValueFactory.fromAnyRef(10))
        .withValue("memcached.useNagleAlgorithm", ConfigValueFactory.fromAnyRef(true));

    new MockUnit(Env.class, Binder.class)
        .expect(cfb)
        .expect(fullprops)
        .expect(bind)
        .run(unit -> {
          new SpyMemcached()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @Test(expected = IllegalArgumentException.class)
  public void enumErr() throws Exception {
    Config config = new SpyMemcached().config()
        .withValue("memcached.server", ConfigValueFactory.fromAnyRef("localhost:11211"))
        .withValue("memcached.enableMetrics", ConfigValueFactory.fromAnyRef("invalid"));

    new MockUnit(Env.class, Binder.class)
        .expect(cfb)
        .expect(defprops)
        .expect(bind)
        .run(unit -> {
          new SpyMemcached()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @Test
  public void configureServers() throws Exception {
    Config config = new SpyMemcached().config()
        .withValue("memcached.server",
            ConfigValueFactory.fromAnyRef(Arrays.asList("localhost:11211", "localhost:11212")));
    new MockUnit(Env.class, Binder.class)
        .expect(cfb)
        .expect(defprops)
        .expect(bind)
        .run(unit -> {
          new SpyMemcached()
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

  @Test
  public void configurer() throws Exception {
    Config config = new SpyMemcached().config()
        .withValue("memcached.server",
            ConfigValueFactory.fromAnyRef(Arrays.asList("localhost:11211", "localhost:11212")));
    new MockUnit(Env.class, Binder.class, AuthDescriptor.class)
        .expect(cfb)
        .expect(defprops)
        .expect(unit -> {
          ConnectionFactoryBuilder cfb = unit.get(ConnectionFactoryBuilder.class);
          expect(cfb.setAuthDescriptor(unit.get(AuthDescriptor.class))).andReturn(cfb);
        })
        .expect(bind)
        .run(unit -> {
          new SpyMemcached()
              .doWith(builder -> {
                builder.setAuthDescriptor(unit.get(AuthDescriptor.class));
              })
              .configure(unit.get(Env.class), config, unit.get(Binder.class));
        });
  }

}
