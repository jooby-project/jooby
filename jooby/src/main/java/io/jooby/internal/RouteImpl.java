/**
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 *    Copyright 2014 Edgar Espina
 */
package io.jooby.internal;

import io.jooby.Renderer;
import io.jooby.Route;

import javax.annotation.Nonnull;
import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.Executor;

public class RouteImpl implements Route {

  private final String method;
  private final String pattern;
  private final Handler handler;
  private Route.Handler pipeline;
  private final Renderer renderer;
  private final Type returnType;
  private Executor executor;
  private final List<String> pathKeys;

  public RouteImpl(String method, String pattern, List<String> pathKeys, Type returnType, Handler handler,
      Route.Handler pipeline, Renderer renderer) {
    this.method = method.toUpperCase();
    this.pattern = pattern;
    this.returnType = returnType;
    this.handler = handler;
    this.pipeline = pipeline;
    this.renderer = renderer;
    this.pathKeys = pathKeys;
  }

  @Override public String pattern() {
    return pattern;
  }

  public List<String> pathKeys() {
    return pathKeys;
  }

  @Override public String method() {
    return method;
  }

  public Executor executor() {
    return executor;
  }

  RouteImpl executor(Executor executor) {
    this.executor = executor;
    return this;
  }

  @Override public Handler handler() {
    return handler;
  }

  @Override public Route.Handler pipeline() {
    return pipeline;
  }

  @Override public Renderer renderer() {
    return renderer;
  }

  @Nonnull @Override public Type returnType() {
    return returnType;
  }

  @Override public String toString() {
    return method + " " + pattern;
  }

  public void pipeline(Route.Handler pipeline) {
    this.pipeline = pipeline;
    this.executor = null;
  }
}
