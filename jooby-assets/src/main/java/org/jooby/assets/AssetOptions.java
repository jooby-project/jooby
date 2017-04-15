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

import java.util.List;
import java.util.Map;

import org.jooby.Route;
import org.jooby.internal.RoutePattern;

import com.google.common.collect.ImmutableList;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import com.typesafe.config.ConfigValueFactory;

/**
 * Base class for {@link AssetProcessor} and {@link AssetAggregator}.
 * Just keep state and handle aseets options.
 *
 * @author edgar
 */
public class AssetOptions {

  private Config options = ConfigFactory.empty();

  public AssetOptions set(final String name, final Object value) {
    requireNonNull(name, "Option's name is required.");
    options = options.withValue(name, ConfigValueFactory.fromAnyRef(value));
    return this;
  }

  public AssetOptions set(final Config options) {
    this.options = requireNonNull(options, "Options are required.").withFallback(this.options);
    return this;
  }

  public Map<String, Object> options() {
    return options.withoutPath("excludes").root().unwrapped();
  }

  @SuppressWarnings("unchecked")
  public boolean excludes(final String path) {
    Object value = get("excludes");
    if (value == null) {
      return false;
    }
    List<String> excludes;
    if (value instanceof List) {
      excludes = (List<String>) value;
    } else {
      excludes = ImmutableList.of(value.toString());
    }
    String spath = Route.normalize(path);
    return excludes.stream()
        .map(it -> new RoutePattern("GET", it))
        .filter(pattern -> pattern.matcher("GET" + spath).matches())
        .findFirst()
        .isPresent();
  }

  @SuppressWarnings("unchecked")
  public <T> T get(final String name) {
    requireNonNull(name, "Option's name is required.");
    if (options.hasPath(name)) {
      return (T) options.getAnyRef(name);
    }
    return null;
  }

}
