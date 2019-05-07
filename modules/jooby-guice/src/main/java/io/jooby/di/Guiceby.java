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
package io.jooby.di;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import io.jooby.Environment;
import io.jooby.Extension;
import io.jooby.Jooby;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class Guiceby implements Extension {

  private Injector injector;
  private List<Module> modules = new ArrayList<>();

  public Guiceby(@Nonnull Injector injector) {
    this.injector = injector;
  }

  public Guiceby(@Nonnull Module... modules) {
    Stream.of(modules).forEach(this.modules::add);
  }

  @Override public boolean lateinit() {
    return true;
  }

  @Override public void install(@Nonnull Jooby application) {
    if (injector == null) {
      Environment env = application.getEnvironment();
      modules.add(new JoobyModule(application));
      Stage stage = env.isActive("dev", "test") ? Stage.DEVELOPMENT : Stage.PRODUCTION;
      injector = Guice.createInjector(stage, modules);
    }
    application.registry(new GuiceRegistry(injector));
  }
}
