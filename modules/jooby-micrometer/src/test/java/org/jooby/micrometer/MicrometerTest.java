package org.jooby.micrometer;

import com.google.inject.Binder;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;
import io.micrometer.atlas.AtlasMeterRegistry;
import io.micrometer.core.instrument.Clock;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.composite.CompositeMeterRegistry;
import io.micrometer.core.instrument.config.MeterRegistryConfig;
import io.micrometer.core.instrument.dropwizard.DropwizardMeterRegistry;
import io.micrometer.core.instrument.simple.SimpleConfig;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.datadog.DatadogMeterRegistry;
import io.micrometer.ganglia.GangliaMeterRegistry;
import io.micrometer.graphite.GraphiteMeterRegistry;
import io.micrometer.influx.InfluxMeterRegistry;
import io.micrometer.jmx.JmxMeterRegistry;
import io.micrometer.newrelic.NewRelicMeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.micrometer.signalfx.SignalFxMeterRegistry;
import io.micrometer.statsd.StatsdMeterRegistry;
import io.micrometer.wavefront.WavefrontMeterRegistry;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isA;
import org.jooby.Env;
import org.jooby.funzy.Throwing;
import org.jooby.test.MockUnit;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CompositeMeterRegistry.class, Micrometer.class})
public class MicrometerTest {

  private MockUnit.Block mainRegistry = unit -> {
    CompositeMeterRegistry registry = unit.constructor(CompositeMeterRegistry.class)
        .build();
    unit.registerMock(CompositeMeterRegistry.class, registry);
    Binder binder = unit.get(Binder.class);

    AnnotatedBindingBuilder abbmr = unit.mock(AnnotatedBindingBuilder.class);
    abbmr.toInstance(registry);
    abbmr.toInstance(registry);

    expect(binder.bind(MeterRegistry.class)).andReturn(abbmr);
    expect(binder.bind(CompositeMeterRegistry.class)).andReturn(abbmr);
  };

  private MockUnit.Block onStop = unit -> {
    CompositeMeterRegistry registry = unit.get(CompositeMeterRegistry.class);
    registry.close();
    Env env = unit.get(Env.class);
    expect(env.onStop(unit.capture(Throwing.Runnable.class))).andReturn(env);
  };

  private MockUnit.Block closeOnStop = unit -> {
    unit.captured(Throwing.Runnable.class).get(0).run();
  };

  @Test
  public void defaults() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(newRegistry(SimpleMeterRegistry.class, SimpleConfig.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void doWith() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, SimpleMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries(SimpleMeterRegistry.class))
        .expect(onStop)
        .expect(addRegistry(SimpleMeterRegistry.class))
        .run(unit -> {
          new Micrometer()
              .doWith(registry -> {
                registry.add(unit.get(SimpleMeterRegistry.class));
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void simple() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, SimpleMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(SimpleMeterRegistry.class))
        .expect(bindRegistry(SimpleMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .simple(conf -> {
                return unit.get(SimpleMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void atlas() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, AtlasMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(AtlasMeterRegistry.class))
        .expect(bindRegistry(AtlasMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .atlas(conf -> {
                return unit.get(AtlasMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void datadog() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, DatadogMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(DatadogMeterRegistry.class))
        .expect(bindRegistry(DatadogMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .datadog(conf -> {
                return unit.get(DatadogMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void dropwizard() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, DropwizardMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(DropwizardMeterRegistry.class))
        .expect(bindRegistry(DropwizardMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .dropwizard(conf -> {
                return unit.get(DropwizardMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void ganglia() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, GangliaMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(GangliaMeterRegistry.class))
        .expect(bindRegistry(GangliaMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .ganglia(conf -> {
                return unit.get(GangliaMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void graphite() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, GraphiteMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(GraphiteMeterRegistry.class))
        .expect(bindRegistry(GraphiteMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .graphite(conf -> {
                return unit.get(GraphiteMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void influx() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, InfluxMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(InfluxMeterRegistry.class))
        .expect(bindRegistry(InfluxMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .influx(conf -> {
                return unit.get(InfluxMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void jmx() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, JmxMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(JmxMeterRegistry.class))
        .expect(bindRegistry(JmxMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .jmx(conf -> {
                return unit.get(JmxMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void newrelic() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, NewRelicMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(NewRelicMeterRegistry.class))
        .expect(bindRegistry(NewRelicMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .newrelic(conf -> {
                return unit.get(NewRelicMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void prometheus() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, PrometheusMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(PrometheusMeterRegistry.class))
        .expect(bindRegistry(PrometheusMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .prometheus(conf -> {
                return unit.get(PrometheusMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void signalfx() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, SignalFxMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(SignalFxMeterRegistry.class))
        .expect(bindRegistry(SignalFxMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .signalfx(conf -> {
                return unit.get(SignalFxMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void statsd() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, StatsdMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(StatsdMeterRegistry.class))
        .expect(bindRegistry(StatsdMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .statsd(conf -> {
                return unit.get(StatsdMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void wavefront() throws Exception {
    new MockUnit(Env.class, Config.class, Binder.class, WavefrontMeterRegistry.class)
        .expect(mainRegistry)
        .expect(registries())
        .expect(addRegistry(WavefrontMeterRegistry.class))
        .expect(bindRegistry(WavefrontMeterRegistry.class))
        .expect(onStop)
        .run(unit -> {
          new Micrometer()
              .wavefront(conf -> {
                return unit.get(WavefrontMeterRegistry.class);
              })
              .configure(unit.get(Env.class), unit.get(Config.class), unit.get(Binder.class));
        }, closeOnStop);
  }

  @Test
  public void simpleConf() {
    doConf("simple", Micrometer::simple);
  }

  @Test
  public void dropwizardConf() {
    doConf("dropwizard", Micrometer::dropwizard);
  }

  @Test
  public void atlasConf() {
    String name = "atlas";
    assertEquals(null, Micrometer.atlas(ConfigFactory.empty()).get("step"));
    assertEquals("PT5M", Micrometer.atlas(conf(name)).get("step"));
    assertEquals("PT5M", Micrometer.atlas(conf(name)).get(name + ".step"));
    assertEquals(null, Micrometer.atlas(conf(name)).get("foo"));
  }

  @Test
  public void datadogConf() {
    doConf("datadog", Micrometer::datadog);
  }

  @Test
  public void gangliaConf() {
    doConf("ganglia", Micrometer::ganglia);
  }

  @Test
  public void graphiteConf() {
    doConf("graphite", Micrometer::graphite);
  }

  @Test
  public void influxConf() {
    doConf("influx", Micrometer::influx);
  }

  @Test
  public void jmxConf() {
    doConf("jmx", Micrometer::jmx);
  }

  @Test
  public void newrelicConf() {
    doConf("newrelic", Micrometer::newrelic);
  }

  @Test
  public void prometheusConf() {
    doConf("prometheus", Micrometer::prometheus);
  }

  @Test
  public void signalfxConf() {
    doConf("signalfx", Micrometer::signalfx);
  }

  @Test
  public void statsdConf() {
    doConf("statsd", Micrometer::statsd);
  }

  @Test
  public void wavefrontConf() {
    doConf("wavefront", Micrometer::wavefront);
  }

  public <T extends MeterRegistryConfig> void doConf(String name, Function<Config, T> creator) {
    assertEquals(name, creator.apply(ConfigFactory.empty()).prefix());
    assertEquals(null, creator.apply(ConfigFactory.empty()).get("step"));
    assertEquals("PT5M", creator.apply(conf(name)).get("step"));
    assertEquals("PT5M", creator.apply(conf(name)).get(name + ".step"));
    assertEquals(null, creator.apply(conf(name)).get("foo"));
  }

  private Config conf(String name) {
    return ConfigFactory.empty()
        .withValue("micrometer." + name + ".step", ConfigValueFactory.fromAnyRef("PT5M"));
  }

  private MockUnit.Block addRegistry(Class type) {
    return unit -> {
      CompositeMeterRegistry registry = unit.get(CompositeMeterRegistry.class);
      expect(registry.add((MeterRegistry) unit.get(type))).andReturn(registry);
    };
  }

  private MockUnit.Block bindRegistry(Class type) {
    return unit -> {
      Object registry = unit.get(type);
      AnnotatedBindingBuilder abb = unit.mock(AnnotatedBindingBuilder.class);
      abb.toInstance(registry);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(registry.getClass())).andReturn(abb);
    };
  }

  private MockUnit.Block newRegistry(Class type, Class conftype) {
    return unit -> {
      Object registry = unit.constructor(type)
          .args(conftype, Clock.class)
          .build(isA(conftype), eq(Clock.SYSTEM));

      unit.registerMock(type, registry);

      CompositeMeterRegistry main = unit.get(CompositeMeterRegistry.class);
      expect(main.add((MeterRegistry) registry)).andReturn(main);

      AnnotatedBindingBuilder abb = unit.mock(AnnotatedBindingBuilder.class);
      abb.toInstance(registry);

      Binder binder = unit.get(Binder.class);
      expect(binder.bind(registry.getClass())).andReturn(abb);
    };
  }

  private MockUnit.Block registries(Class... registries) {
    return unit -> {
      CompositeMeterRegistry registry = unit.get(CompositeMeterRegistry.class);
      Set<MeterRegistry> r = new LinkedHashSet<>();
      for (Class t : registries) {
        r.add((MeterRegistry) unit.get(t));
      }
      expect(registry.getRegistries()).andReturn(r);
    };
  }
}
