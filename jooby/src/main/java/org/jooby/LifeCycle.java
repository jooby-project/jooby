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

public interface LifeCycle {

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

  default LifeCycle managed(final Managed managed) {
    onStart(managed::start);

    onStop(managed::stop);
    return this;
  }

  default LifeCycle managed(final Class<?> managed) {
    lifeCycleAnnotation(managed, PostConstruct.class)
        .ifPresent(it -> onStart(app -> it.accept(app.require(managed))));

    lifeCycleAnnotation(managed, PreDestroy.class)
        .ifPresent(it -> onStop(app -> it.accept(app.require(managed))));
    return this;
  }

  /**
   * Add a start task, useful for initialize and/or start services at startup time.
   *
   * You should ONLY call this method while the application is been initialized, usually executing
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)}.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  LifeCycle onStart(CheckedConsumer<Jooby> task);

  /**
   * Add a start task, useful for initialize and/or start services at startup time.
   *
   * You should ONLY call this method while the application is been initialized, usually executing
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)}.
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
   * Add a stop task, useful for cleanup and/or stop service at stop time.
   *
   * You should ONLY call this method while the application is been initialized, usually executing
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)}.
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
   * Add a stop task, useful for cleanup and/or stop service at stop time.
   *
   * You should ONLY call this method while the application is been initialized, usually executing
   * {@link Jooby.Module#configure(Env, Config, com.google.inject.Binder)}.
   *
   * The behaviour of this method once application has been initialized is <code>undefined</code>.
   *
   * @param task Task to run.
   * @return This env.
   */
  LifeCycle onStop(CheckedConsumer<Jooby> task);

}
