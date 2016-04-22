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
package org.jooby.rx;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.Route;
import org.jooby.Routes;
import org.jooby.exec.Exec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import rx.Completable;
import rx.Observable;
import rx.Scheduler;
import rx.Single;
import rx.Subscriber;
import rx.plugins.RxJavaPlugins;
import rx.plugins.RxJavaSchedulersHook;
import rx.schedulers.Schedulers;

/**
 * <h1>rxjava</h1>
 * <p>
 * Reactive programming via <a href="https://github.com/ReactiveX/RxJava">rxjava</a>.
 * </p>
 * <p>
 * RxJava is a Java VM implementation of <a href="http://reactivex.io">Reactive Extensions</a>: a
 * library for composing asynchronous and event-based programs by using observable sequences.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>map route operator that converts {@link Observable} (and family) into {@link Deferred} API.
 * </li>
 * <li>
 * manage the lifecycle of {@link Scheduler schedulers} and make sure they go down on application
 * shutdown time.
 * </li>
 * <li>set a default server thread pool with the number of available processors.</li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 *
 * ...
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx());
 *
 *   get("/", req -> Observable.from("reactive programming in jooby!"))
 *      .map(Rx.rx());
 * }
 * }</pre>
 *
 * <h2>how it works?</h2>
 * <p>
 * Previous example is translated to:
 * </p>
 * <pre>{@code
 * {
 *   use(new Rx());
 *
 *   get("/", req -> {
 *
 *    return new Deferred(deferred -> {
 *      Observable.from("reactive programming in jooby!")
 *        .subscribe(deferred::resolve, deferred::reject);
 *    });
 *
 *   });
 * }
 * }</pre>
 *
 * <p>
 * Translation is done with the {@link Rx#rx()} route operator. If you are a
 * <a href="https://github.com/ReactiveX/RxJava">rxjava</a> programmer then you don't need to worry
 * for learning a new API and semantic. The {@link Rx#rx()} route operator deal and take cares of
 * the {@link Deferred} API.
 * </p>
 *
 * <h2>rx()</h2>
 * <p>
 * We just learn that we are not force to learn a new API, just write
 * <a href="https://github.com/ReactiveX/RxJava">rxjava</a> code. That's cool!
 * </p>
 *
 * <p>
 * But.. what if you have 10 routes? 50 routes?
 * </p>
 *
 * <pre>{@code
 *
 * ...
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx());
 *
 *   get("/1", req -> Observable...)
 *      .map(Rx.rx());
 *
 *   get("/2", req -> Observable...)
 *      .map(Rx.rx());
 *
 *   ....
 *
 *   get("/N", req -> Observable...)
 *      .map(Rx.rx());
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
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx());
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
 *   }).map(Rx.rx());
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
 * <h2>rx()+scheduler</h2>
 * <p>
 * You can provide a {@link Scheduler} to the {@link #rx()} operator:
 * </p>
 *
 * <pre>{@code
 * ...
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx());
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
 *   }).map(Rx.rx(Schedulers::io));
 * }
 * }</pre>
 *
 * <p>
 * All the routes here will {@link Observable#subscribeOn(Scheduler) subscribe-on} the
 * provided {@link Scheduler}.
 * </p>
 *
 * <h2>schedulers</h2>
 * <p>
 * This module provides the default {@link Scheduler} from
 * <a href="https://github.com/ReactiveX/RxJava">rxjava</a>. But also let you define your own
 * {@link Scheduler scheduler} using the {@link Exec} module.
 * </p>
 *
 * <pre>
 * rx.schedulers.io = forkjoin
 * rx.schedulers.computation = fixed
 * rx.schedulers.newThread = "fixed = 10"
 * </pre>
 *
 * <p>
 * The previous example defines a:
 * </p>
 * <ul>
 * <li>forkjoin pool for {@link Schedulers#io()}</li>
 * <li>fixed thread pool equals to the number of available processors for
 * {@link Schedulers#computation()}</li>
 * <li>fixed thread pool with a max of 10 for {@link Schedulers#newThread()}</li>
 * </ul>
 *
 * <p>
 * Of course, you can define/override all, some or none of them. In any case the {@link Scheduler}
 * will be shutdown at application shutdown time.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public class Rx extends Exec {

  static class DeferredSubscriber extends Subscriber<Object> {

    private Deferred deferred;

    private AtomicBoolean done = new AtomicBoolean(false);

    public DeferredSubscriber(final Deferred deferred) {
      this.deferred = deferred;
    }

    @Override
    public void onCompleted() {
      if (done.compareAndSet(false, true)) {
        deferred.resolve((Object) null);
      }
      deferred = null;
    }

    @Override
    public void onError(final Throwable cause) {
      done.set(true);
      deferred.reject(cause);
    }

    @Override
    public void onNext(final Object value) {
      if (done.compareAndSet(false, true)) {
        deferred.resolve(value);
      }
    }
  }

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  public Rx() {
    super("rx.schedulers");
    // daemon by default.
    daemon(true);
  }

  /**
   * Map a rx object like {@link Observable}, {@link Single} or {@link Completable} into a
   * {@link Deferred} object.
   *
   * <pre>{@code
   * ...
   * import org.jooby.rx.Rx;
   * ...
   *
   * {
   *   use(new Rx());
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
   *   }).map(Rx.rx());
   * }
   * }</pre>
   *
   * @return A new mapper.
   */
  public static Route.Mapper<Object> rx() {
    return rx(Optional.empty());
  }

  /**
   * Map a rx object like {@link Observable}, {@link Single} or {@link Completable} into a
   * {@link Deferred} object.
   *
   * <pre>{@code
   * ...
   * import org.jooby.rx.Rx;
   * ...
   *
   * {
   *   use(new Rx());
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
   *   }).map(Rx.rx());
   * }
   * }</pre>
   *
   * @param subscribeOn An scheduler to subscribeOn.
   * @return A new mapper.
   */
  public static Route.Mapper<Object> rx(final Supplier<Scheduler> subscribeOn) {
    return rx(Optional.of(subscribeOn));
  }

  @SuppressWarnings("unchecked")
  private static Route.Mapper<Object> rx(final Optional<Supplier<Scheduler>> subscribeOn) {
    return value -> Match(value).of(
        /** Observable : */
        Case(instanceOf(Observable.class),
            it -> new Deferred(deferred -> subscribeOn
                .map(s -> it.subscribeOn(s.get()))
                .orElse(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Single : */
        Case(instanceOf(Single.class),
            it -> new Deferred(deferred -> subscribeOn
                .map(s -> it.subscribeOn(s.get()))
                .orElse(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Completable : */
        Case(instanceOf(Completable.class),
            it -> new Deferred(deferred -> subscribeOn
                .map(s -> it.subscribeOn(s.get()))
                .orElse(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Ignore */
        Case($(), value));
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    // dump rx.* as system properties
    conf.getConfig("rx")
        .withoutPath("schedulers").entrySet()
        .forEach(
            e -> System.setProperty("rx." + e.getKey(), e.getValue().unwrapped().toString()));
    Map<String, Executor> executors = new HashMap<>();
    super.configure(env, conf, binder, executors::put);

    /**
     * Side effects of global/evil static state. Hack to turn off some of this errors.
     */
    trySchedulerHook(executors);

    // shutdown schedulers: silent shutdown in dev mode between app reloads
    env.onStop(() -> {
      try {
        Schedulers.shutdown();
      } catch (Throwable ex) {
        log.debug("Schedulers.shutdown() resulted in error", ex);
      }
    });
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "rx.conf");
  }

  private void trySchedulerHook(final Map<String, Executor> executors) {
    RxJavaPlugins plugins = RxJavaPlugins.getInstance();
    try {
      plugins.registerSchedulersHook(new ExecSchedulerHook(executors));
    } catch (IllegalStateException ex) {
      // there is a scheduler hook already, check if ours and ignore the exception
      RxJavaSchedulersHook hook = plugins.getSchedulersHook();
      if (!(hook instanceof ExecSchedulerHook)) {
        throw ex;
      }
    }
  }

}
