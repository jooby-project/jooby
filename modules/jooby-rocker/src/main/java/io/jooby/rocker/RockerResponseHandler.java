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

import com.fizzed.rocker.RockerModel;
import io.jooby.Reified;
import io.jooby.ResponseHandler;
import io.jooby.Route;

import java.lang.reflect.Type;

class RockerResponseHandler implements ResponseHandler {
  @Override public boolean matches(Type type) {
    return RockerModel.class.isAssignableFrom(Reified.rawType(type));
  }

  @Override public Route.Handler create(Route.Handler next) {
    return new RockerHandler(next);
  }
}
