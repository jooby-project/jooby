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
package org.jooby.mongodb;

import static java.util.Objects.requireNonNull;

import org.jongo.Jongo;
import org.jongo.Mapper;
import org.jongo.marshall.jackson.JacksonMapper;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.internal.mongodb.JongoFactoryImpl;

import com.google.inject.Binder;
import com.typesafe.config.Config;

/**
 * <h1>jongo module</h1>
 * <p>
 * Exposes {@link Jongo} instances to a default database. Or {@link JongoFactory} to use custom or
 * alternative databases.
 * </p>
 *
 * <p>
 * Please note, this module depends on: <code>Mongodb</code> module.
 * </p>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Mongodb());
 *   use(new Jongoby());
 *
 *   get("/", req {@literal ->} {
 *     Jongo jongo = req.require(Jongo.class);
 *     // work with jongo...
 *   });
 * }
 * </pre>
 *
 * <p>
 * Previous example will give you a {@link Jongo} instance connected to the default database,
 * provided by the <code>Mongodb</code> module.
 * </p>
 *
 * <p>
 * Access to alternate database is provided via: {@link JongoFactory}.
 * </p>
 *
 * <pre>
 * {
 *   use(new Mongodb());
 *   use(new Jongoby());
 *
 *   get("/", req {@literal ->} {
 *     Jongo jongo = req.require(JongoFactory.class).get("alternate-db");
 *     // work with jongo...
 *   });
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.8.0
 */
public class Jongoby implements Jooby.Module {

  private Mapper mapper;

  /**
   * Creates a new {@link Jongoby} module and use the given {@link Mapper}.
   *
   * @param mapper {@link Mapper} to use.
   */
  public Jongoby(final Mapper mapper) {
    this.mapper = requireNonNull(mapper, "Mapper is required.");
  }

  /**
   * Creates a new {@link Jongoby} module and use the given {@link Mapper}.
   *
   * @param mapper {@link Mapper} to use.
   */
  public Jongoby(final JacksonMapper.Builder mapper) {
    this(mapper.build());
  }

  /**
   * Creates a new {@link Jongoby} module and use the default mapper.
   */
  public Jongoby() {
    this(new JacksonMapper.Builder());
  }

  @Override
  public void configure(final Env env, final Config config, final Binder binder) {
    binder.bind(Mapper.class).toInstance(mapper);
    binder.bind(Jongo.class).toProvider(JongoFactoryImpl.class);
    binder.bind(JongoFactory.class).to(JongoFactoryImpl.class);
  }

}
