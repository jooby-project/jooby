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

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;
import io.jooby.Jooby;
import io.jooby.ResourceKey;

import java.util.Map;

/**
 * Guice module that provides injection support for Jooby resource objects.
 */
public class ResourceModule extends AbstractModule {
  
  private Jooby app;
  
  public ResourceModule(Jooby app) {
    this.app = app;
  }
  
  @Override
  protected void configure() {
    // bind the available resources as well, supporting the name annotations that may be set
    for(Map.Entry<ResourceKey, Object> entry : app.getResources().entrySet()) {
      ResourceKey key = entry.getKey();
      if(key.getName() != null) {
        bind(key.getType()).annotatedWith(Names.named(key.getName())).toInstance(entry.getValue());
      } else {
        bind(key.getType()).toInstance(entry.getValue());
      }
    }
  }
}
