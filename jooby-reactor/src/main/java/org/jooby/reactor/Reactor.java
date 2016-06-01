/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.reactor;

import static java.util.Objects.requireNonNull;
import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.function.Function;

import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * <h1>reactor</h1>
 * <p>
 * <a href="http://projectreactor.io">Reactor</a> is a second-generation Reactive library for
 * building non-blocking applications on the JVM
 * based on the <a href="http://www.reactive-streams.org">Reactive Streams Specification</a>
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>map route operator that converts {@link Flux} and {@link Mono} into {@link Deferred} API.
 * </li>
 * <li>set a default server thread pool with the number of available processors.</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 *
 * ...
 * import org.jooby.reactor.Reactor;
 * ...
 *
 * {
 *   use(new Reactor());
 *
 *   get("/", req -> Flux.just("reactive programming in jooby!"));
 * }
 * }</pre>
 *
 * <h2>how it works?</h2>
 * <p>
 * Previous example is translated to:
 * </p>
 *
 * <pre>{@code
 * {
 *   use(new Reactor());
 *
 *   get("/", req -> {
 *
 *    return new Deferred(deferred -> {
 *      Flux.just("reactive programming in jooby!")
 *        .consume(deferred::resolve, deferred::reject);
 *    });
 *
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Translation is done via {@link Reactor#reactor()} route mapper. If you are a
 * <a href="http://projectreactor.io">reactor</a> programmer then you don't need to worry
 * for learning a new API and semantic. The {@link Reactor#reactor()} route operator deal and take
 * cares of the {@link Deferred} API.
 * </p>
 *
 * <h2>reactor mapper</h2>
 * <p>
 * Advanced flux/mono configuration is allowed via function adapters:
 * </p>
 *
 * <pre>{@code
 *
 * ...
 * import org.jooby.reactor.Reactor;
 * ...
 *
 * {
 *   use(new Reactor()
 *       .withFlux(f -> f.publishOn(Computations.concurrent())
 *       .withMono(m -> m.publishOn(Computations.concurrent()));
 *
 *   get("/flux", req -> Flux...);
 *
 *   get("/mono", req -> Mono...);
 *
 * }
 * }</pre>
 *
 * <p>
 * Here every Flux/Mono from a route handler will publish on the <code>concurrent</code> scheduler.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
@SuppressWarnings("rawtypes")
public class Reactor implements Jooby.Module {

  private Function<Flux, Flux> flux = Function.identity();

  private Function<Mono, Mono> mono = Function.identity();

  public Reactor withFlux(final Function<Flux, Flux> adapter) {
    this.flux = requireNonNull(adapter, "Flux's adapter is required.");
    return this;
  }

  public Reactor withMono(final Function<Mono, Mono> adapter) {
    this.mono = requireNonNull(adapter, "Mono's adapter is required.");
    return this;
  }

  /**
   * Map a reactor object like {@link Flux} or {@link Mono} into a {@link Deferred} object.
   *
   * <pre>{@code
   * ...
   * import org.jooby.reactor.Reactor;
   * ...
   *
   * {
   *   with(() -> {
   *     get("/lux", req -> Flux...);
   *
   *     get("/mono", req -> Mono...);
   *
   *   }).map(Reactor.reactor(
   *       flux -> flux.publishOn(Computations.concurrent()),
   *       mono -> mono.publishOn(Computations.concurrent()));
   * }
   * }</pre>
   *
   * @param flux A flux adapter.
   * @param mono A mono adapter.
   * @return A new mapper.
   */
  @SuppressWarnings("unchecked")
  public static Route.Mapper<Object> reactor(final Function<Flux, Flux> flux,
      final Function<Mono, Mono> mono) {
    requireNonNull(flux, "Flux's adapter is required.");
    requireNonNull(mono, "Mono's adapter is required.");
    return Route.Mapper.create("reactor", value -> Match(value).of(
        /** Flux: */
        Case(instanceOf(Flux.class),
            it -> new Deferred(deferred -> flux.apply(it)
                .consume(deferred::set, deferred::set))),
        /** Mono: */
        Case(instanceOf(Mono.class),
            it -> new Deferred(deferred -> mono.apply(it)
                .consume(deferred::set, deferred::set))),
        /** Ignore */
        Case($(), value)));

  }

  /**
   * Map a reactor object like {@link Flux} or {@link Mono} into a {@link Deferred} object.
   *
   * <pre>{@code
   * ...
   * import org.jooby.reactor.Reactor;
   * ...
   *
   * {
   *   with(() -> {
   *     get("/lux", req -> Flux...);
   *
   *     get("/mono", req -> Mono...);
   *
   *   }).map(Reactor.reactor(
   *       flux -> flux.publishOn(Computations.concurrent()),
   *       mono -> mono.publishOn(Computations.concurrent()));
   * }
   * }</pre>
   *
   * @param flux A flux adapter.
   * @param mono A mono adapter.
   * @return A new mapper.
   */
  public static Route.Mapper<Object> reactor() {
    return reactor(Function.identity(), Function.identity());
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    env.routes()
        .map(reactor(flux, mono));
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "reactor.conf");
  }
}
