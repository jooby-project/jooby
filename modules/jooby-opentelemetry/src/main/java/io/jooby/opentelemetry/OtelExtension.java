/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.opentelemetry;

import io.jooby.Jooby;
import io.opentelemetry.api.OpenTelemetry;

/**
 * Extension point for OpenTelemetry integrations within a Jooby application.
 *
 * <p>While {@link OtelModule} is responsible for bootstrapping the core OpenTelemetry SDK, this
 * interface allows developers to seamlessly attach secondary instrumentation modules (such as
 * Logback appenders, HikariCP metrics, or Quartz job tracers) to the running SDK.
 *
 * <p><strong>Lifecycle:</strong> Extensions are not executed immediately when passed to the {@code
 * OtelModule} constructor. Instead, their execution is deferred until the Jooby application fires
 * its {@code onStarting} event. This guarantees that the primary OpenTelemetry instance is fully
 * configured and safely registered before any extensions attempt to use it.
 */
@FunctionalInterface
public interface OtelExtension {

  /**
   * Installs and binds the OpenTelemetry extension to the application.
   *
   * @param application The current Jooby application. Extensions can use this to read application
   *     configuration, register internal services, or attach additional lifecycle hooks (e.g.,
   *     closing resources during {@code onStop}).
   * @param openTelemetry The fully constructed and configured OpenTelemetry instance.
   * @throws Exception If the extension fails to initialize or attach its instrumentation.
   */
  void install(Jooby application, OpenTelemetry openTelemetry) throws Exception;
}
