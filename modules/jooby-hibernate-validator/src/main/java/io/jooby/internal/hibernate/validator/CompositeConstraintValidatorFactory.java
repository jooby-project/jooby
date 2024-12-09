/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

import java.util.Deque;
import java.util.LinkedList;

import org.slf4j.Logger;

import io.jooby.Jooby;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

public class CompositeConstraintValidatorFactory implements ConstraintValidatorFactory {
  private final Logger log;
  private final ConstraintValidatorFactory defaultFactory;
  private final Deque<ConstraintValidatorFactory> factories = new LinkedList<>();

  public CompositeConstraintValidatorFactory(
      Jooby registry, ConstraintValidatorFactory defaultFactory) {
    this.log = registry.getLog();
    this.defaultFactory = defaultFactory;
    this.factories.addLast(new RegistryConstraintValidatorFactory(registry));
  }

  public ConstraintValidatorFactory add(ConstraintValidatorFactory factory) {
    this.factories.addFirst(factory);
    return this;
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
    if (isBuiltIn(key)) {
      // use default factory for built-in constraint validators
      return defaultFactory.getInstance(key);
    } else {
      for (var factory : factories) {
        var instance = factory.getInstance(key);
        if (instance != null) {
          return instance;
        }
      }
      // fallback or fail
      return defaultFactory.getInstance(key);
    }
  }

  @Override
  public void releaseInstance(ConstraintValidator<?, ?> instance) {
    if (isBuiltIn(instance.getClass())) {
      defaultFactory.releaseInstance(instance);
    } else {
      if (instance instanceof AutoCloseable closeable) {
        try {
          closeable.close();
        } catch (Exception e) {
          log.debug("Failed to release constraint", e);
        }
      }
    }
  }

  private boolean isBuiltIn(Class<?> key) {
    var name = key.getName();
    return name.startsWith("org.hibernate.validator") || name.startsWith("jakarta.validation");
  }
}
