/**
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
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
