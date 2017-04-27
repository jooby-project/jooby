/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jooby.consul;

import com.google.inject.Binder;
import com.orbitz.consul.AgentClient;
import com.orbitz.consul.Consul;
import com.orbitz.consul.model.agent.ImmutableRegistration;
import com.orbitz.consul.model.agent.Registration;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.jooby.Env;
import org.jooby.Jooby;

import java.text.MessageFormat;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * <p>Consul client module.</p>
 *
 * <p>Exports the {@link Consul} client.</p>
 *
 * <p>Also register the application as a service and setup a health check.</p>
 *
 * <h1>usage</h1>
 *
 * <pre>
 * {
 *   use(new Consulby());
 *
 *   get("/myservice/health", req {@literal ->} {
 *     Consul consul = require(Consul.class);
 *     List<ServiceHealth> serviceHealths = consul.healthClient()
 *       .getHealthyServiceInstances("myservice")
 *       .getResponse();
 *     return serviceHealths;
 *   });
 * }
 * </pre>
 *
 * <h1>configuration</h1>
 *
 * <p>Configuration is done via <code>.conf</code>.</p>
 *
 * <p>For example, one can change the consul endpoint url,
 * change the advertised service host, and disable registration health check:</p>
 *
 * <pre>
 * consul.default.url = "http://consul.internal.domain.com:8500"
 * consul.default.register.host = 10.0.0.2
 * consul.default.register.check = null
 * </pre>
 *
 * <p>or, disable the automatic registration feature completely:</p>
 *
 * <pre>
 * consul.default.register = null
 * </pre>
 *
 * @since 1.1.1
 */
public class Consulby implements Jooby.Module {

  private final String name;

  private Consumer<Consul.Builder> consulBuilderConsumer;
  private Consumer<ImmutableRegistration.Builder> registrationBuilderConsumer;

  /**
   * A new {@link Consulby} instance, with the default config name: <code>default</code>.
   */
  public Consulby() {
    this("default");
  }

  /**
   * <p>A new {@link Consulby} instance, with a provided config name.</p>
   *
   * <p>The module can be instantiated more than one time to allow connecting to many Consul installations:</p>
   *
   * <pre>
   * {
   *   use(new Consulby("consul1"));
   *   use(new Consulby("consul2"));
   * }
   * </pre>
   *
   * <p>Since the module will fallback on the <code>consul.default</code> config prefix,
   * it is possible to only override the desired properties in the <code>.conf</code>,
   * for example, here, disabling health check only for `consul2`:</p>
   *
   * <pre>
   * consul.consul1.url = "http://consul1.internal.domain.com:8500"
   *
   * consul.consul2.url = "http://consul2.internal.domain.com:8500"
   * consul.consul2.register.check = null
   * </pre>
   *
   * @param name A config name
   */
  public Consulby(String name) {
    this.name = Objects.requireNonNull(name, "A consul config name is required.");
  }

  /**
   * <p>{@link Consul} object can be configured programmatically:</p>
   *
   * <pre>
   * {
   *   use(new Consulby()
   *     .withConsulBuilder(consulBuilder -> {
   *       consulBuilder.withPing(false);
   *       consulBuilder.withBasicAuth("admin", "changeme");
   *     }));
   * }
   * </pre>
   *
   * @param consulBuilderConsumer A {@link Consumer} that accepts {@link Consul.Builder}
   * @return This {@link Consulby} to allow chaining
   */
  public Consulby withConsulBuilder(Consumer<Consul.Builder> consulBuilderConsumer) {
    this.consulBuilderConsumer = consulBuilderConsumer;
    return this;
  }

  /**
   * <p>{@link Registration} object can be configured programmatically:</p>
   *
   * <pre>
   * {
   *   use(new Consulby()
   *     .withRegistrationBuilder(registrationBuilder -> {
   *       registrationBuilder.enableTagOverride(true);
   *       registrationBuilder.id("custom-service-id");
   *     }));
   * }
   * </pre>
   *
   * @param registrationBuilderConsumer A {@link Consumer} that accepts {@link ImmutableRegistration.Builder}
   * @return This {@link Consulby} to allow chaining
   */
  public Consulby withRegistrationBuilder(Consumer<ImmutableRegistration.Builder> registrationBuilderConsumer) {
    this.registrationBuilderConsumer = registrationBuilderConsumer;
    return this;
  }

  @Override
  public void configure(Env env, Config config, Binder binder) throws Throwable {

    Config consulConfig = config.getConfig("consul.default");
    if (!name.equals("default") && config.hasPath("consul." + name)) {
      consulConfig = config.getConfig("consul." + name).withFallback(consulConfig);
    }

    Consul.Builder consulBuilder = Consul.builder()
        .withUrl(consulConfig.getString("url"));

    if (consulBuilderConsumer != null) {
      consulBuilderConsumer.accept(consulBuilder);
    }

    Consul consul = consulBuilder.build();

    env.onStop(consul::destroy);

    env.serviceKey().generate(Consul.class, name, k -> binder.bind(k).toInstance(consul));

    if (consulConfig.hasPath("register")) {

      Config registerConfig = consulConfig.getConfig("register");

      ImmutableRegistration.Builder registrationBuilder = ImmutableRegistration.builder()
          .name(registerConfig.getString("name"))
          .address(registerConfig.getString("host"))
          .port(registerConfig.getInt("port"))
          .tags(registerConfig.getStringList("tags"))
          .id(UUID.randomUUID().toString());

      if (registerConfig.hasPath("check")) {

        Config checkConfig = registerConfig.getConfig("check");

        String http = MessageFormat.format("http://{0}:{1,number,####}{2}",
            registerConfig.getString("host"),
            registerConfig.getInt("port"),
            checkConfig.getString("path"));

        Registration.RegCheck check = Registration.RegCheck.http(http,
            checkConfig.getDuration("interval", TimeUnit.SECONDS),
            checkConfig.getDuration("timeout", TimeUnit.SECONDS));

        registrationBuilder.check(check);

        String response = checkConfig.getString("response");
        env.router().get(checkConfig.getString("path"), () -> response);
      }

      if (registrationBuilderConsumer != null) {
        registrationBuilderConsumer.accept(registrationBuilder);
      }

      Registration registration = registrationBuilder.build();

      AgentClient agentClient = consul.agentClient();
      env.onStarted(() -> agentClient.register(registration));
      env.onStop(() -> agentClient.deregister(registration.getId()));
    }
  }

  @Override
  public Config config() {
    return ConfigFactory.parseResources(getClass(), "consul.conf");
  }
}
