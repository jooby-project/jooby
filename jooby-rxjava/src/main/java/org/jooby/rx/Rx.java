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

import static java.util.Objects.requireNonNull;
import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;

import org.jooby.Deferred;
import org.jooby.Env;
import org.jooby.Route;
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
 * Reactive programming via <a href="https://github.com/ReactiveX/RxJava">RxJava library</a>.
 * </p>
 * <p>
 * RxJava is a Java VM implementation of <a href="http://reactivex.io">Reactive Extensions</a>: a
 * library for composing asynchronous and event-based programs by using observable sequences.
 * </p>
 *
 * <h2>exports</h2>
 * <ul>
 * <li>Route mapper that converts {@link Observable} (and family) into {@link Deferred} API.
 * </li>
 * <li>
 * manage the lifecycle of {@link Scheduler schedulers} and make sure they go down on application
 * shutdown time.
 * </li>
 * </ul>
 *
 * <h2>usage</h2>
 * <pre>{@code
 * ...
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx());
 *
 *   get("/", req -> Observable.from("reactive programming in jooby!"));
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
 * Translation is done via {@link Rx#rx()} route mapper. If you are a
 * <a href="https://github.com/ReactiveX/RxJava">rxjava</a> programmer then you don't need to worry
 * for learning a new API and semantic. The {@link Rx#rx()} route mapper deal and take cares of
 * the {@link Deferred} API.
 * </p>
 *
 * <h2>rx mapper</h2>
 * <p>
 * Advanced observable configuration is allowed via function adapter:
 * </p>
 *
 * <pre>{@code
 * ...
 * import org.jooby.rx.Rx;
 * ...
 *
 * {
 *   use(new Rx()
 *     .withObservable(observable -> observable.observeOn(Scheduler.io()),
 *     .withSingle(single -> single.observeOn(Scheduler.io()),
 *     .withCompletable(completable -> completable.observeOn(Scheduler.io()));
 *
 *   get("/observable", req -> Observable...);
 *
 *   get("/single", req -> Single...);
 *
 *   ....
 *
 *   get("/completable", req -> Completable...);
 *
 * }
 * }</pre>
 *
 * <p>
 * Here every Observable/Single/Completable from a route handler will observe on the <code>io</code>
 * scheduler.
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
@SuppressWarnings("rawtypes")
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

  private Function<Observable, Observable> observable = Function.identity();

  private Function<Single, Single> single = Function.identity();

  private Function<Completable, Completable> completable = Function.identity();

  /**
   * Creates a new {@link Rx} module.
   */
  public Rx() {
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
   *   with(() -> {
   *     get("/1", req -> Observable...);
   *
   *     get("/2", req -> Single...);
   *
   *     ....
   *
   *     get("/N", req -> Completable...);
   *
   *   }).map(Rx.rx());
   * }
   * }</pre>
   *
   * @return A new mapper.
   */
  public static Route.Mapper<Object> rx() {
    return rx(Function.identity(), Function.identity());
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
   *   }).map(Rx.rx(
   *       observable -> observable.observeOn(Scheduler.io()),
   *       single -> single.observeOn(Scheduler.io()),
   *       completable -> completable.observeOn(Scheduler.io())));
   * }
   * }</pre>
   *
   * @param observable Observable adapter.
   * @param single Single adapter.
   * @return A new mapper.
   */
  public static Route.Mapper<Object> rx(final Function<Observable, Observable> observable,
      final Function<Single, Single> single) {
    return rx(observable, single, Function.identity());
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
   *   }).map(Rx.rx(
   *       observable -> observable.observeOn(Scheduler.io()),
   *       single -> single.observeOn(Scheduler.io()),
   *       completable -> completable.observeOn(Scheduler.io())));
   * }
   * }</pre>
   *
   * @param observable Observable adapter.
   * @param single Single adapter.
   * @param completable Completable adapter.
   * @return A new mapper.
   */
  @SuppressWarnings("unchecked")
  public static Route.Mapper<Object> rx(final Function<Observable, Observable> observable,
      final Function<Single, Single> single, final Function<Completable, Completable> completable) {
    requireNonNull(observable, "Observable's adapter is required.");
    requireNonNull(single, "Single's adapter is required.");
    requireNonNull(completable, "Completable's adapter is required.");

    return Route.Mapper.create("rx", v -> Match(v).of(
        /** Observable : */
        Case(instanceOf(Observable.class),
            it -> new Deferred(deferred -> observable.apply(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Single : */
        Case(instanceOf(Single.class),
            it -> new Deferred(deferred -> single.apply(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Completable : */
        Case(instanceOf(Completable.class),
            it -> new Deferred(deferred -> completable.apply(it)
                .subscribe(new DeferredSubscriber(deferred)))),
        /** Ignore */
        Case($(), v)));
  }

  /**
   * Apply the given function adapter to observables returned by routes:
   *
   * <pre>{@code
   * {
   *   use(new Rx().withObservable(observable -> observable.observeOn(Schedulers.io())));
   *
   *   get("observable", -> {
   *     return Observable...
   *   });
   * }
   * }</pre>
   *
   * @param adapter Observable adapter.
   * @return This module.
   */
  public Rx withObservable(final Function<Observable, Observable> adapter) {
    this.observable = requireNonNull(adapter, "Observable's adapter is required.");
    return this;
  }

  /**
   * Apply the given function adapter to single returned by routes:
   *
   * <pre>{@code
   * {
   *   use(new Rx().withSingle(observable -> observable.observeOn(Schedulers.io())));
   *
   *   get("single", -> {
   *     return Single...
   *   });
   * }
   * }</pre>
   *
   * @param adapter Single adapter.
   * @return This module.
   */
  public Rx withSingle(final Function<Single, Single> adapter) {
    this.single = requireNonNull(adapter, "Single's adapter is required.");
    return this;
  }

  /**
   * Apply the given function adapter to completable returned by routes:
   *
   * <pre>{@code
   * {
   *   use(new Rx().withObservable(observable -> observable.observeOn(Schedulers.io())));
   *
   *   get("completable", -> {
   *     return Completable...
   *   });
   * }
   * }</pre>
   *
   * @param adapter Completable adapter.
   * @return This module.
   */
  public Rx withCompletable(final Function<Completable, Completable> adapter) {
    this.completable = requireNonNull(adapter, "Completable's adapter is required.");
    return this;
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

    env.router()
        .map(rx(observable, single, completable));

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
