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
package org.jooby.assets;

import static java.util.Objects.requireNonNull;

import java.util.Map;

import org.jooby.MediaType;

import com.google.common.base.CaseFormat;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * <h2>asset processor</h2>
 * <p>
 * Checks, validate and/or modify asset contents. An {@link AssetProcessor} is usually provided as a
 * separated dependency.
 * </p>
 *
 * <h3>how to use it?</h3>
 * <p>
 * First thing to do is to add the dependency:
 * </p>
 * <pre>
 *   &lt;dependency&gt;
 *     &lt;groupId&gt;org.jooby&lt;/groupId&gt;
 *     &lt;artifactId&gt;jooby-assets-my-processor&lt;/artifactId&gt;
 *     &lt;scope&gt;provided&lt;/scope&gt;
 *   &lt;/dependency&gt;
 * </pre>
 *
 * <p>
 * Did you see the <strong>provided</strong> scope? We just need the processor for development,
 * because assets are processed on the fly. For <code>prod</code>, assets are processed at
 * built-time via Maven plugin, at runtime we don't need this. This also, helps to keep our
 * dependencies and the jar size to minimum.
 * </p>
 *
 * <p>
 * Now we have the dependency all we have to do is to add it to our pipeline:
 * </p>
 *
 * <pre>
 * assets {
 *   pipeline: {
 *     dev: [my-processor]
 *   }
 * }
 * </pre>
 *
 * <h3>configuration</h3>
 * <p>
 * It is possible to configure or set options too:
 * </p>
 *
 * <pre>
 * assets {
 *   pipeline: {
 *     dev: [my-processor]
 *     dist: [my-processor]
 *   }
 *
 *   my-processor {
 *     foo: bar
 *   }
 * }
 * </pre>
 *
 * <p>
 * Previous example, set a <code>foo</code> property to <code>bar</code>! Options can be set
 * per environment too:
 * </p>
 *
 * <pre>
 * assets {
 *   pipeline: {
 *     dev: [my-processor]
 *     dist: [my-processor]
 *   }
 *
 *   my-processor {
 *     dev {
 *       bar: bar
 *     }
 *     dist {
 *       foo: bar
 *     }
 *     foo: foo
 *   }
 * }
 * </pre>
 *
 * <p>
 * Here, in <code>dev</code> processor has two properties: <code>foo:foo</code> and
 * <code>bar:bar</code>, while in <code>dist</code> the processor only has <code>foo:bar</code>
 * </p>
 *
 * <h3>binding</h3>
 * <p>
 * The <code>my-processor</code> will be resolved it to: <code>org.jooby.assets.MyProcessor</code>
 * class. The processor name is converted to <code>MyProcessor</code>, it converts the hyphenated
 * name to upper camel and by default processors are defined in the <code>org.jooby.assets</code>
 * package.
 * </p>
 * <p>
 * A custom binding is provided via the <code>class</code> property:
 * </p>
 * <pre>
 * assets {
 *   pipeline: {
 *     dev: [my-processor]
 *     dist: [my-processor]
 *   }
 *
 *   my-processor {
 *     class: whatever.i.Want
 *   }
 * }
 * </pre>
 *
 * @author edgar
 * @since 0.11.0
 */
public abstract class AssetProcessor {

  private Config options = ConfigFactory.empty();;

  public AssetProcessor set(final String name, final Object value) {
    requireNonNull(name, "Option's name is required.");
    options = options.withValue(name, ConfigValueFactory.fromAnyRef(value));
    return this;
  }

  public AssetProcessor set(final Config options) {
    this.options = requireNonNull(options, "Options are required.").withFallback(this.options);
    return this;
  }

  public Map<String, Object> options() {
    return options.withoutPath("excludes").root().unwrapped();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name) {
    requireNonNull(name, "Option's name is required.");
    if (options.hasPath(name)) {
      return (T) options.getAnyRef(name);
    }
    return null;
  }

  public final String name() {
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_HYPHEN, getClass().getSimpleName());
  }

  public abstract boolean matches(final MediaType type);

  public abstract String process(String filename, String source, Config conf) throws Exception;

  @Override
  public String toString() {
    return name();
  }
}
