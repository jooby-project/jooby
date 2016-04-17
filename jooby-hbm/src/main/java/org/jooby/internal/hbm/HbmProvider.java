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
package org.jooby.internal.hbm;

import java.util.Map;

import javax.inject.Provider;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.jooby.Managed;

import javaslang.Lazy;

public class HbmProvider implements Provider<HibernateEntityManagerFactory>, Managed {

  private Lazy<HibernateEntityManagerFactory> emf;

  public HbmProvider(final HbmUnitDescriptor descriptor, final Map<Object, Object> config) {
    this.emf = Lazy.of(() -> {
      EntityManagerFactoryBuilder builder = Bootstrap
          .getEntityManagerFactoryBuilder(descriptor, config);
      return (HibernateEntityManagerFactory) builder.build();
    });
  }

  @Override
  public void start() {
  }

  @Override
  public HibernateEntityManagerFactory get() {
    return emf.get();
  }

  @Override
  public void stop() {
    if (emf != null) {
      emf.get().close();
      emf = null;
    }
  }
}
