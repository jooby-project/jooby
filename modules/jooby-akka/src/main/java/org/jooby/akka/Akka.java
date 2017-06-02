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
package org.jooby.akka;

import static java.util.Objects.requireNonNull;

import org.jooby.Env;
import org.jooby.Jooby.Module;

import com.google.inject.Binder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import akka.actor.ActorSystem;

/**
 * <h1>akka</h1>
 * <p>
 * Concurrent and distributed applications via <a href="http://akka.io">Akka</a>.
 * </p>
 *
 * <h2>exposes</h2>
 * <ul>
 * <li>An {@link ActorSystem}</li>
 * </ul>
 *
 * <h2>usage</h2>
 *
 * <pre>
 * {
 *   use(new Akka());
 *
 *   get("/akka", promise((req, deferred) {@literal ->} {
 *     ActorSystem sys = req.require(ActorSystem.class);
 *     ActorRef actor = sys.actorOf(...);
 *     // send the deferred to the actor
 *     actor.tell(deferred, actor);
 *   });
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.10.0
 */
public class Akka implements Module {

  private String name;

  /**
   * Creates a new {@link Akka} module.
   *
   * @param name Name of the {@link ActorSystem}.
   */
  public Akka(final String name) {
    this.name = requireNonNull(name, "ActorSystem's name is required.");
  }

  /**
   * Creates a new {@link Akka} module.
   */
  public Akka() {
    this("default");
  }

  @Override
  public void configure(final Env env, final Config conf, final Binder binder) {
    ActorSystem sys = ActorSystem.create(name, conf);

    env.serviceKey().generate(ActorSystem.class, name, syskey -> {
      binder.bind(syskey).toInstance(sys);
    });
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "akka.conf");
  }
}
