/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.rocker;

import com.fizzed.rocker.runtime.RockerRuntime;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;
import io.jooby.MediaType;

import javax.annotation.Nonnull;

public class Rockerby implements Extension {

  private Boolean reloading;

  public Rockerby reloading(boolean reloading) {
    this.reloading = reloading;
    return this;
  }

  @Override public void install(@Nonnull Jooby application) throws Exception {
    Environment env = application.getEnvironment();
    RockerRuntime runtime = RockerRuntime.getInstance();
    boolean reloading = this.reloading == null
        ? (env.isActive("dev") && runtime.isReloadingPossible())
        : this.reloading.booleanValue();
    runtime.setReloading(reloading);
    // response handler
    application.responseHandler(new RockerResponseHandler());
    // renderer
    application.renderer(MediaType.html, new RockerRenderer());
  }
}
