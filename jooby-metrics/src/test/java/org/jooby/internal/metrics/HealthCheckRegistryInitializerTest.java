package org.jooby.internal.metrics;

import static org.easymock.EasyMock.expectLastCall;

import java.util.Map;

import com.codahale.metrics.health.HealthCheck;
import org.jooby.test.MockUnit;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.codahale.metrics.health.HealthCheckRegistry;
import com.google.common.collect.ImmutableMap;

@RunWith(PowerMockRunner.class)
@PrepareForTest(HealthCheckRegistryInitializer.class)
public class HealthCheckRegistryInitializerTest {

  @Test
  public void register() throws Exception {
    new MockUnit(HealthCheckRegistry.class, HealthCheck.class)
      .expect(unit -> {
        HealthCheckRegistry registry = unit.get(HealthCheckRegistry.class);
        registry.register("h", unit.get(HealthCheck.class));
        expectLastCall();
      })
      .run(unit -> {
        Map<String, HealthCheck> checks = ImmutableMap.of("h", unit.get(HealthCheck.class));
        HealthCheckRegistryInitializer hcri = new HealthCheckRegistryInitializer(
        unit.get(HealthCheckRegistry.class), checks);
      });
  }
}
