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
package org.jooby.hbm;

import static com.google.common.base.Preconditions.checkState;

import javax.inject.Provider;

import org.hibernate.jpa.HibernateEntityManagerFactory;
import org.hibernate.jpa.boot.spi.EntityManagerFactoryBuilder;

class HbmProvider implements Provider<HibernateEntityManagerFactory> {

  private EntityManagerFactoryBuilder builder;

  private HibernateEntityManagerFactory emf;

  public HbmProvider(final EntityManagerFactoryBuilder builder) {
    this.builder = builder;
  }

  public void start() {
    emf = (HibernateEntityManagerFactory) builder.build();
  }

  @Override
  public HibernateEntityManagerFactory get() {
    checkState(emf != null, "Hbm wasn't started yet");
    return emf;
  }

  public void stop() {
    if (emf != null) {
      emf.close();
    }
  }
}
