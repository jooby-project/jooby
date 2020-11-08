/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import javax.annotation.Nonnull;

import com.fizzed.rocker.RockerOutputFactory;
import com.fizzed.rocker.runtime.RockerRuntime;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.ServiceRegistry;

/**
 * Rocker module. It requires some build configuration setup which are documented in the web site.
 * Please refer to https://jooby.io/modules/rocker for more details.
 *
 * @author edgar
 * @since 2.0.0
 */
public class RockerModule implements Extension {

  private Boolean reloading;

  private int bufferSize = ByteBufferOutput.BUFFER_SIZE;

  private boolean reuseBuffer;

  /**
   * Turn on/off autoreloading of template for development.
   *
   * @param reloading True for turning on.
   * @return This module.
   */
  public @Nonnull RockerModule reloading(boolean reloading) {
    this.reloading = reloading;
    return this;
  }

  /**
   * Configure buffer size to use while rendering. The buffer can grow ups when need it, so this
   * option works as a hint to allocate initial memory.
   *
   * @param bufferSize Buffer size.
   * @return This module.
   */
  public @Nonnull RockerModule useBuffer(int bufferSize) {
    this.bufferSize = bufferSize;
    return this;
  }

  /**
   * Allow simple reuse of raw byte buffers. It is usually used through <code>ThreadLocal</code>
   * variable pointing to instance of {@link ByteBufferOutput}.
   *
   * @param reuseBuffer True for reuse the buffer. Default is: <code>false</code>
   * @return This module.
   */
  public RockerModule reuseBuffer(boolean reuseBuffer) {
    this.reuseBuffer = reuseBuffer;
    return this;
  }

  @Override public void install(@Nonnull Jooby application) {
    Environment env = application.getEnvironment();
    RockerRuntime runtime = RockerRuntime.getInstance();
    boolean reloading = this.reloading == null
        ? (env.isActive("dev") && runtime.isReloadingPossible())
        : this.reloading.booleanValue();
    RockerOutputFactory<ByteBufferOutput> factory = ByteBufferOutput.factory(bufferSize);
    if (reuseBuffer) {
      factory = ByteBufferOutput.reuse(factory);
    }
    runtime.setReloading(reloading);
    // response handler
    application.responseHandler(new RockerResponseHandler(factory));
    // renderer
    application.encoder(new RockerMessageEncoder(factory));
    // factory
    ServiceRegistry services = application.getServices();
    services.put(RockerOutputFactory.class, factory);
  }
}
