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
package org.jooby.jade;

import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import de.neuland.jade4j.JadeConfiguration;
import de.neuland.jade4j.template.ClasspathTemplateLoader;
import org.jooby.Env;
import org.jooby.Jooby;
import org.jooby.Renderer;
import org.jooby.View;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Exposes a {@link Renderer}.
 *
 * <h1>usage</h1>
 * <p>
 * It is pretty straightforward:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jade());
 *
 *   get("/", req {@literal ->} Results.html("index").put("model", new MyModel());
 * }
 * </pre>
 * <p>
 * public/index.html:
 * </p>
 *
 * <pre>
 * pre= model
 * </pre>
 *
 * <p>
 * Templates are loaded from root of classpath: <code>/</code> and must end with: <code>.jade</code>
 * file extension.
 * </p>
 *
 * <h1>configuration</h1>
 * <h2>application.conf</h2>
 * <p>
 * Just add a <code>jade.*</code> option to your <code>application.conf</code> file:
 * </p>
 *
 * <pre>
 * jade.prettyprint: true
 * jade.suffix: .html
 * </pre>
 *
 * <h1>template loader</h1>
 * <p>
 * Templates are loaded from the root of classpath and must end with <code>.jade</code>. You can
 * change the default template location and extensions too:
 * </p>
 *
 * <pre>
 * {
 *   use(new Jade("/", ".jade"));
 * }
 * </pre>
 *
 * <h1>cache</h1>
 * <p>
 * Cache is OFF when <code>env=dev</code> (useful for template reloading), otherwise is ON and does not expire.
 * </p>
 * <p>
 * That's all folks! Enjoy it!!!
 * </p>
 *
 * @since 1.0.0
 */
public class Jade implements Jooby.Module {

  private final Logger log = LoggerFactory.getLogger(getClass());

  @Override
  public Config config() {
    return ConfigFactory.empty();
  }

  @Override
  public void configure(Env env, Config config, Binder binder) {
    JadeConfiguration jadeConfiguration = new JadeConfiguration();
    boolean caching = !env.name().equals("dev");
    boolean prettyPrint = config.hasPath("jade.prettyprint") && config.getBoolean("jade.prettyprint");

    jadeConfiguration.setCaching(caching);
    jadeConfiguration.setPrettyPrint(prettyPrint);

    Map<String, Object> sharedVariables = new HashMap<>(1);
    sharedVariables.put("env", env);
    jadeConfiguration.setSharedVariables(sharedVariables);

    jadeConfiguration.setTemplateLoader(new ClasspathTemplateLoader());

    binder.bind(JadeConfiguration.class).toInstance(jadeConfiguration);

    String suffix = config.hasPath("jade.suffix") ? config.getString("jade.suffix") : ".jade";
    View.Engine engine = new Engine(jadeConfiguration, suffix);
    Multibinder.newSetBinder(binder, Renderer.class).addBinding().toInstance(engine);

    log.info("Using jade renderer with options prettyprint:{}, caching:{}, suffix:{}", prettyPrint, caching, suffix);
  }
}
