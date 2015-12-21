package org.jooby.internal.metrics;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.health.HealthCheck;
import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest({HealthCheckRegistryProvider.class, HealthCheckRegistry.class })
public class HealthCheckRegistryProviderTest {

  @Test
  public void register() throws Exception {
    new MockUnit(HealthCheck.class)
        .expect(unit -> {
          HealthCheckRegistry registry = unit.constructor(HealthCheckRegistry.class)
              .build();
          unit.registerMock(HealthCheckRegistry.class, registry);

          registry.register("check", unit.get(HealthCheck.class));
        })
        .run(unit -> {
          Map<String, HealthCheck> checks = ImmutableMap.of("check", unit.get(HealthCheck.class));
          assertEquals(unit.get(HealthCheckRegistry.class),
              new HealthCheckRegistryProvider(checks).get());
        });
  }
}
