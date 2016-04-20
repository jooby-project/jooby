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
package org.jooby;

import static javaslang.API.$;
import static javaslang.API.Case;
import static javaslang.API.Match;
import static javaslang.Predicates.instanceOf;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.typesafe.config.Config;

import javaslang.control.Try.CheckedConsumer;
import javaslang.control.Try.CheckedRunnable;

/**
 * <h2>life cycle</h2>
 * <p>
 * Listen for application start and stop events. Useful for starting/stopping services.
 * </p>
 *
 * <h3>onStart/onStop events</h3>
 * <p>
 * Start/stop callbacks are accessible via application:
 * </p>
 * <pre>{@code
 * {
 *    onStart(() -> {
 *      log.info("starting app");
 *    });
 *
 *    onStop(() -> {
 *      log.info("stopping app");
 *    });
 * }
 * }</pre>
 *
 * <p>
 * Or via module:
 * </p>
 * <pre>{@code
 * public class MyModule implements Jooby.Module {
 *
 *   public void configure(Env env, Config conf, Binder binder) {
 *     env.onStart(() -> {
 *       log.info("starting module");
 *     });
 *
 *     env.onStop(() -> {
 *       log.info("stopping module");
 *     });
 *   }
 * }
 * }</pre>
 *
 * <h3>callbacks order</h3>
 * <p>
 * Callback order is preserved:
 * </p>
 *
 * <pre>{@code
 * {
 *   onStart(() -> {
 *     log.info("first");
 *   });
 *
 *   onStart(() -> {
 *     log.info("second");
 *   });
 *
 *   onStart(() -> {
 *     log.info("third");
 *   });
 * }
 * }</pre>
 * <p>
 * Order is useful for service dependencies, like ServiceB should be started after ServiceA.
 * </p>
 *
 * <h3>service registry</h3>
 * <p>
 * You can also request for a service and start or stop it:
 * </p>
 *
 * <pre>{@code
 * {
 *   onStart(registry -> {
 *     MyService service = registry.require(MyService.class);
 *     service.start();
 *   });
 *
 *   onStop(registry -> {
 *     MyService service = registry.require(MyService.class);
 *     service.stop();
 *   });
 * }
 * }</pre>
 *
 * <h3>PostConstruct/PreDestroy annotations</h3>
 * <p>
 * If you prefer the annotation way... you can too:
 * </p>
 *
 * <pre>{@code
 *
 * &#64;Singleton
 * public class MyService {
 *
 *   &#64;PostConstruct
 *   public void start() {
 *   }
 *
 *   &#64;PreDestroy
 *   public void stop() {
 *   }
 * }
 *
 * App.java:
 *
 * {
 *   lifeCycle(MyService.class);
 * }
 *
 * }</pre>
 *
 * <p>
 * It works as expected just make sure <code>MyService</code> is a <strong>Singleton</strong>
 * object.
 * </p>
 *
 * @author edgar
 * @since 1.0.0.CR3
 */
public interface LifeCycle {

  /**
   * Find a single method annotated with the given annotation in the provided type.
   *
   * @param rawType The type to look for a method.
   * @param annotation Annotation to look for.
   * @return A callback to the method. Or empty.
   */
  static Optional<CheckedConsumer<Object>> lifeCycleAnnotation(final Class<?> rawType,
      final Class<? extends Annotation> annotation) {
    for (Method method : rawType.getDeclaredMethods()) {
      if (method.getAnnotation(annotation) != null) {
        int mods = method.getModifiers();
        if (Modifier.isStatic(mods)) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not be static: " + method);
        }
        if (!Modifier.isPublic(mods)) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method must be public: " + method);
        }
        if (method.getParameterCount() > 0) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not accept arguments: " + method);
        }
        if (method.getReturnType() != void.class) {
          throw new IllegalArgumentException(annotation.getSimpleName()
              + " method should not return anything: " + method);
        }
        return Optional.of(owner -> {
          try {
            method.setAccessible(true);
            method.invoke(owner);
          } catch (InvocationTargetException ex) {
            throw Match(ex.getTargetException()).of(
                Case(instanceOf(RuntimeException.class), x -> x),
                Case($(), x -> new IllegalStateException(
                    "execution of " + annotation + " resulted in error", x)));
          }
        });
      }
    }
    return Optional.empty();
  };

  /**
   * Add to lifecycle the given service. Any method annotated with {@link PostConstruct} or
   * {@link PreDestroy} will be executed at application startup or shutdown time.
   *
   * The service must be a Singleton object.
   *
   * <pre>{@code
   *
   * &#64;Singleton
   * public class MyService {
   *
   *   &#64;PostConstruct
   *   public void start() {
   *   }
   *
   *   &#64;PreDestroy
   *   public void stop() {
   *   }
   * }
   *
   * App.java:
   *
   * {
   *   lifeCycle(MyService.class);
   * }
   *
   * }</pre>
   *
   * You should ONLY call this method while the application is been initialized or while
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)} is been executed.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param service Service type. Must be a singleton object.
   * @return This instance.
   */
  default LifeCycle lifeCycle(final Class<?> service) {
    lifeCycleAnnotation(service, PostConstruct.class)
        .ifPresent(it -> onStart(app -> it.accept(app.require(service))));

    lifeCycleAnnotation(service, PreDestroy.class)
        .ifPresent(it -> onStop(app -> it.accept(app.require(service))));
    return this;
  }

  /**
   * Add a start lifecycle event, useful for initialize and/or start services at startup time.
   *
   * You should ONLY call this method while the application is been initialized or while
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)} is been executed.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  LifeCycle onStart(CheckedConsumer<Registry> task);

  /**
   * Add a start lifecycle event, useful for initialize and/or start services at startup time.
   *
   * You should ONLY call this method while the application is been initialized or while
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)} is been executed.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  default LifeCycle onStart(final CheckedRunnable task) {
    return onStart(app -> task.run());
  }

  /**
   * Add a stop lifecycle event, useful for cleanup and/or stop service at stop time.
   *
   * You should ONLY call this method while the application is been initialized or while
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)} is been executed.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  default LifeCycle onStop(final CheckedRunnable task) {
    return onStop(app -> task.run());
  }

  /**
   * Add a stop lifecycle event, useful for cleanup and/or stop service at stop time.
   *
   * You should ONLY call this method while the application is been initialized or while
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)} is been executed.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  LifeCycle onStop(CheckedConsumer<Registry> task);

}
