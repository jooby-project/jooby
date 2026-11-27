/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rainbowgum;

import java.util.List;
import java.util.Optional;

import io.jstach.rainbowgum.LogConfig;
import io.jstach.rainbowgum.LogProperties;
import io.jstach.rainbowgum.RainbowGum;
import io.jstach.rainbowgum.ServiceRegistry;
import io.jstach.rainbowgum.spi.RainbowGumServiceProvider;

/** Queues logging before Jooby's application environment is available. */
public final class RainbowGumBootstrap implements RainbowGumServiceProvider.RainbowGumProvider {
  /** For service loading. */
  public RainbowGumBootstrap() {}

  @Override
  public Optional<RainbowGum> provide(LogConfig config) {
    // Only this queued instance is marked as Jooby's, not every service loaded config.
    var registry = ServiceRegistry.of();
    RainbowGumService.markBootstrap(registry);
    var properties =
        LogProperties.of(
            List.of(
                LogProperties.StandardProperties.SYSTEM_PROPERTIES, RainbowGumService.DEFAULTS));
    var bootstrap = LogConfig.builder().properties(properties).serviceRegistry(registry).build();
    return Optional.of(RainbowGum.queued(bootstrap));
  }

  @Override
  public int priority() {
    // Application providers win; their Rainbow Gum is left under the application's control.
    return Integer.MIN_VALUE;
  }
}
