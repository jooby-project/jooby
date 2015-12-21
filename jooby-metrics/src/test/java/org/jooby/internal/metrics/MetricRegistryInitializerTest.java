package org.jooby.internal.metrics;

import static org.easymock.EasyMock.expect;

import java.util.Map;
import java.util.Set;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.ConsoleReporter;
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Reporter;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

@RunWith(PowerMockRunner.class)
@PrepareForTest({MetricRegistryInitializer.class, HealthCheckRegistry.class })
public class MetricRegistryInitializerTest {

  @Test
  public void register() throws Exception {
    new MockUnit(MetricRegistry.class, Metric.class, Reporter.class, ConsoleReporter.class)
        .expect(unit -> {
          MetricRegistry registry = unit.get(MetricRegistry.class);

          expect(registry.register("m", unit.get(Metric.class)))
              .andReturn(unit.get(Metric.class));
        })
        .expect(unit -> {
          unit.get(ConsoleReporter.class).close();
        })
        .run(unit -> {
          Map<String, Metric> metrics = ImmutableMap.of("m", unit.get(Metric.class));
          Set<Reporter> reporters = ImmutableSet.of(unit.get(Reporter.class),
              unit.get(ConsoleReporter.class));
          MetricRegistryInitializer mri = new MetricRegistryInitializer(
              unit.get(MetricRegistry.class), metrics, reporters);
          mri.start();
          mri.stop();
          mri.stop();
        });
  }
}
