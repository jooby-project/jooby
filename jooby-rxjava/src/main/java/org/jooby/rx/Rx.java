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
import org.jooby.exec.Exec;

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

  public Rx() {
    super("rx.schedulers");
    // daemon by default.
    daemon(true);
  }

  public static Route.Mapper<Object> rx() {
    return rx(Optional.empty());
  }

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
    if (conf.hasPath("rx")) {
      conf.getConfig("rx")
          .withoutPath("schedulers").entrySet()
          .forEach(
              e -> System.setProperty("rx." + e.getKey(), e.getValue().unwrapped().toString()));
    }
    Map<String, Executor> executors = new HashMap<>();
    super.configure(env, conf, binder, executors::put);
    RxJavaPlugins plugins = RxJavaPlugins.getInstance();
    plugins.registerSchedulersHook(schedulerHook(executors));
    // shutdown schedulers: silent shutdown on tests we got a NoClassDefFoundError: Could not
    // initialize class rx.internal.util.RxRingBuffer
    env.onStop(Schedulers::shutdown);
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "rx.conf");
  }

  static RxJavaSchedulersHook schedulerHook(final Map<String, Executor> executors) {
    return new RxJavaSchedulersHook() {
      @Override
      public Scheduler getComputationScheduler() {
        return Optional.ofNullable(executors.get("computation"))
            .map(Schedulers::from)
            .orElse(null);
      }

      @Override
      public Scheduler getIOScheduler() {
        return Optional.ofNullable(executors.get("io"))
            .map(Schedulers::from)
            .orElse(null);
      }

      @Override
      public Scheduler getNewThreadScheduler() {
        return Optional.ofNullable(executors.get("newThread"))
            .map(Schedulers::from)
            .orElse(null);
      }
    };
  }

}
