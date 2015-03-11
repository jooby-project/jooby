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

import static com.google.common.base.Preconditions.checkState;

import java.util.Map;

import javax.inject.Provider;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.Bootstrap;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;
import org.jooby.Managed;

public class HbmProvider implements Provider<HibernateEntityManagerFactory>, Managed {

  private HibernateEntityManagerFactory emf;

  private HbmUnitDescriptor descriptor;

  private Map<Object, Object> config;

  public HbmProvider(final HbmUnitDescriptor descriptor, final Map<Object, Object> config) {
    this.descriptor = descriptor;
    this.config = config;
  }

  @Override
  public void start() {
    EntityManagerFactoryBuilder builder = Bootstrap
        .getEntityManagerFactoryBuilder(descriptor, config);
    emf = (HibernateEntityManagerFactory) builder.build();
  }

  @Override
  public HibernateEntityManagerFactory get() {
    checkState(emf != null, "Hbm wasn't started yet");
    return emf;
  }

  @Override
  public void stop() {
    if (emf != null) {
      emf.close();
    }
  }
}
