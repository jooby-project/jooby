/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import com.fizzed.rocker.RockerOutputFactory;
import com.fizzed.rocker.runtime.RockerRuntime;
import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.ExecutionMode;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;
import io.jooby.internal.BufferedRockerOutputImpl;
import io.jooby.internal.HeapRockerOutput;

/**
 * Rocker module. It requires some build configuration setup which are documented in the web site.
 * Please refer to https://jooby.io/modules/rocker for more details.
 *
 * @author edgar
 * @since 2.0.0
 */
public class RockerModule implements Extension {
  private Boolean reloading;
  private final Charset charset;
  private Boolean reuseBuffer;

  public RockerModule(@NonNull Charset charset) {
    this.charset = charset;
  }

  public RockerModule() {
    this(StandardCharsets.UTF_8);
  }

  /**
   * Turn on/off autoreloading of template for development.
   *
   * @param reloading True for turning on.
   * @return This module.
   */
  public RockerModule reloading(boolean reloading) {
    this.reloading = reloading;
    return this;
  }

  /**
   * Allow simple reuse of raw byte buffers. It is usually used through <code>ThreadLocal</code>
   * variable pointing to instance of {@link io.jooby.output.BufferedOutput}.
   *
   * @param reuseBuffer True for reuse the buffer. Enabled by default when running in {@link
   *     ExecutionMode#EVENT_LOOP}.
   * @return This module.
   */
  public RockerModule reuseBuffer(boolean reuseBuffer) {
    this.reuseBuffer = reuseBuffer;
    return this;
  }

  @Override
  public void install(@NonNull Jooby application) {
    var env = application.getEnvironment();
    var runtime = RockerRuntime.getInstance();
    boolean reloading =
        this.reloading == null
            ? (env.isActive("dev") && runtime.isReloadingPossible())
            : this.reloading;
    var reuseBuffer =
        this.reuseBuffer == null
            ? application.getExecutionMode() == ExecutionMode.EVENT_LOOP
            : this.reuseBuffer;
    RockerOutputFactory<BufferedRockerOutput> factory =
        reuseBuffer
            ? HeapRockerOutput.factory(charset, application.getOutputFactory())
            : BufferedRockerOutputImpl.factory(charset, application.getOutputFactory());
    runtime.setReloading(reloading);
    // renderer
    application.encoder(new RockerMessageEncoder(factory));
    // factory
    ServiceRegistry services = application.getServices();
    services.put(RockerOutputFactory.class, factory);
  }
}
