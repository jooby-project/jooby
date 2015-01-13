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
package org.jooby.hbs;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import com.github.jknack.handlebars.ValueResolver;
import com.typesafe.config.Config;

/**
 * Handlebars value resolver for accessing config properties from {@link Config} objects.
 *
 * @author edgar
 */
public class ConfigValueResolver implements ValueResolver {

  @Override
  public Object resolve(final Object context, final String name) {
    if (context instanceof Config) {
      Config config = (Config) context;
      if (config.hasPath(name)) {
        return config.getAnyRef(name);
      }
    }
    return UNRESOLVED;
  }

  @Override
  public Object resolve(final Object context) {
    if (context instanceof Config) {
      return context;
    }
    return UNRESOLVED;
  }

  @Override
  public Set<Entry<String, Object>> propertySet(final Object context) {
    if (context instanceof Config) {
      return ((Config) context).root().unwrapped().entrySet();
    }
    return Collections.emptySet();
  }

}
