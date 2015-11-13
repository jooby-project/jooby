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
package org.jooby.hbv;

import static java.util.Objects.requireNonNull;

import java.io.Closeable;
import java.io.IOException;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;

class HbvConstraintValidatorFactory implements ConstraintValidatorFactory {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private Injector injector;

  public HbvConstraintValidatorFactory(final Injector injector) {
    this.injector = requireNonNull(injector, "Injector is required.");
  }

  @Override
  public <T extends ConstraintValidator<?, ?>> T getInstance(final Class<T> key) {
    return injector.getInstance(key);
  }

  @Override
  public void releaseInstance(final ConstraintValidator<?, ?> instance) {
    if (instance instanceof Closeable) {
      try {
        ((Closeable) instance).close();
      } catch (IOException ex) {
        log.debug("Can't close constraint validator", ex);
      }
    }
  }

}
