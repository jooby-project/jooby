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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;

import org.hibernate.validator.HibernateValidatorConfiguration;
import org.jooby.Managed;

import com.google.inject.Injector;

class HbvFactory implements Managed, Provider<Validator> {

  private HibernateValidatorConfiguration conf;

  private Validator validator;

  private ValidatorFactory factory;

  @Inject
  public HbvFactory(final HibernateValidatorConfiguration conf, final Injector injector) {
    this.conf = requireNonNull(conf, "Validation config is required.");

    conf.constraintValidatorFactory(new HbvConstraintValidatorFactory(injector));
  }

  @Override
  public Validator get() {
    requireNonNull(validator, "No validator available");
    return validator;
  }

  @Override
  public void start() throws Exception {
    this.factory = conf.buildValidatorFactory();
    validator = factory.getValidator();
  }

  @Override
  public void stop() throws Exception {
    if (factory != null) {
      factory.close();
      validator = null;
      factory = null;
    }
  }

}
