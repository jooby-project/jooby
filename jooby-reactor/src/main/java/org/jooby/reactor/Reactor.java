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

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.Optional;
import java.util.function.Supplier;

import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Route;
import org.jooby.Routes;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;

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
 *   get("/", req -> Flux.just("reactive programming in jooby!"))
 *      .map(Reactor.reactor());
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
 * Translation is done with the {@link Reactor#reactor()} route operator. If you are a
 * <a href="http://projectreactor.io">reactor</a> programmer then you don't need to worry
 * for learning a new API and semantic. The {@link Reactor#reactor()} route operator deal and take
 * cares of the {@link Deferred} API.
 * </p>
 *
 * <h2>reactor()</h2>
 * <p>
 * We just learn that we are not force to learn a new API, just write
 * <a href="http://projectreactor.io">reactor</a> code. That's cool!
 * </p>
 *
 * <p>
 * But.. what if you have 10 routes? 50 routes?
 * </p>
 *
 * <pre>{@code
 *
 * ...
 * import org.jooby.reactor.Reactor;
 * ...
 *
 * {
 *   use(new Reactor());
 *
 *   get("/1", req -> Observable...)
 *      .map(Reactor.reactor());
 *
 *   get("/2", req -> Observable...)
 *      .map(Reactor.reactor());
 *
 *   ....
 *
 *   get("/N", req -> Observable...)
 *      .map(Reactor.reactor());
 * }
 * }</pre>
 *
 * <p>
 * This is better than written N routes using the {@link Deferred} API route by route... but still
 * there is one more option to help you (and your fingers) to right less code:
 * </p>
 *
 * <pre>{@code
 * ...
 * import org.jooby.reactor.Reactor;
 * ...
 *
 * {
 *   use(new Reactor());
 *
 *   with(() -> {
 *     get("/1", req -> Observable...);
 *
 *     get("/2", req -> Observable...);
 *
 *     ....
 *
 *     get("/N", req -> Observable...);
 *
 *   }).map(Reactor.reactor());
 * }
 * }</pre>
 *
 * <p>
 * <strong>Beautiful, hugh?</strong>
 * </p>
 *
 * <p>
 * The {@link Routes#with(Runnable) with} operator let you group any number of routes and apply
 * common attributes and/or operator to all them!!!
 * </p>
 *
 * <h2>reactor()+scheduler</h2>
 * <p>
 * You can provide a {@link Scheduler} to the {@link #reactor(Supplier)} operator:
 * </p>
 *
 * <pre>{@code
 * ...
 * import org.jooby.reactor.Reactor;
 * ...
 *
 * {
 *   use(new Reactor());
 *
 *   with(() -> {
 *     get("/1", req -> Observable...);
 *
 *     get("/2", req -> Observable...);
 *
 *     ....
 *
 *     get("/N", req -> Observable...);
 *
 *   }).map(Reactor.reactor(Computations::concurrent));
 * }
 * }</pre>
 *
 * <p>
 * All the routes here will {@link Flux#subscribeOn(Scheduler) subscribe-on} the provided
 * {@link Scheduler}.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public class Reactor implements Jooby.Module {

  /**
   * Map a reactor object like {@link Flux} or {@link Mono} into a {@link Deferred} object.
   *
   * <pre>{@code
   * ...
   * import org.jooby.reactor.Reactor;
   * ...
   *
   * {
   *   use(new Reactor());
   *
   *   with(() -> {
   *     get("/1", req -> Observable...);
   *
   *     get("/2", req -> Observable...);
   *
   *     ....
   *
   *     get("/N", req -> Observable...);
   *
   *   }).map(Reactor.reactor());
   * }
   * }</pre>
   *
   * @param subscribeOn An scheduler to subscribeOn.
   * @return A new mapper.
   */
  public static Route.Mapper<Object> reactor(final Supplier<Scheduler> subscribeOn) {
    return reactor(Optional.of(subscribeOn));
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
   *   use(new Reactor());
   *
   *   with(() -> {
   *     get("/1", req -> Observable...);
   *
   *     get("/2", req -> Observable...);
   *
   *     ....
   *
   *     get("/N", req -> Observable...);
   *
   *   }).map(Reactor.reactor());
   * }
   * }</pre>
   *
   * @return A new mapper.
   */
  public static Route.Mapper<Object> reactor() {
    return reactor(Optional.empty());
  }

  @SuppressWarnings("unchecked")
  private static Route.Mapper<Object> reactor(final Optional<Supplier<Scheduler>> subscribeOn) {
    return value -> Match(value).of(
        /** Flux: */
        Case(instanceOf(Flux.class),
            it -> new Deferred(deferred -> subscribeOn
                .map(s -> it.subscribeOn(s.get()))
                .orElse(it)
                .consume(deferred::set, deferred::set))),
        /** Mono: */
        Case(instanceOf(Mono.class),
            it -> new Deferred(deferred -> subscribeOn
                .map(s -> it.subscribeOn(s.get()))
                .orElse(it)
                .consume(deferred::set, deferred::set))),
        /** Ignore */
        Case($(), value));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "reactor.conf");
  }
}
