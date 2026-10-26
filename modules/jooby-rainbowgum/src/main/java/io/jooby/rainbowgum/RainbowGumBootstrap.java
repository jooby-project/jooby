/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rainbowgum;

import java.util.List;
import java.util.Optional;

import io.jstach.rainbowgum.LogAlerts;
import io.jstach.rainbowgum.LogConfig;
import io.jstach.rainbowgum.LogProperties;
import io.jstach.rainbowgum.RainbowGum;
import io.jstach.rainbowgum.ServiceRegistry;
import io.jstach.rainbowgum.spi.RainbowGumServiceProvider;

/** Queues logging before Jooby's application environment is available. */
public final class RainbowGumBootstrap
    implements RainbowGumServiceProvider.RainbowGumProvider,
        RainbowGumServiceProvider.PropertiesProvider {
  /** For service loading. */
  public RainbowGumBootstrap() {}

  @Override
  public List<LogProperties> provideProperties(ServiceRegistry registry, LogAlerts alerts) {
    RainbowGumService.register(registry);
    return List.of(RainbowGumService.DEFAULTS);
  }

  @Override
  public Optional<RainbowGum> provide(LogConfig config) {
    return Optional.of(RainbowGum.queued(config));
  }
}
