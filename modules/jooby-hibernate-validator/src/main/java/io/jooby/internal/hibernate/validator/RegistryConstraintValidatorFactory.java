/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.hibernate.validator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.jooby.Registry;
import io.jooby.exception.RegistryException;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;

public class RegistryConstraintValidatorFactory implements ConstraintValidatorFactory {
  private final Logger log = LoggerFactory.getLogger(getClass());
  private final Registry registry;

  public RegistryConstraintValidatorFactory(Registry registry) {
    this.registry = registry;
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
    try {
      return registry.require(key);
    } catch (RegistryException notfound) {
      return null;
    }
  }

  @Override
  public void releaseInstance(ConstraintValidator<?, ?> instance) {
    if (instance instanceof AutoCloseable closeable) {
      try {
        closeable.close();
      } catch (Exception e) {
        log.debug("Failed to release constraint", e);
      }
    }
  }
}
