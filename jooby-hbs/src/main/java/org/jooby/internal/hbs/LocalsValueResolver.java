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
package org.jooby.internal.hbs;

import java.util.Collections;
import java.util.Map.Entry;
import java.util.Set;

import org.jooby.Locals;

import com.github.jknack.handlebars.ValueResolver;

/**
 * Handlebars value resolver for accessing to {@link Locals}, like request and session objects.
 *
 * @author edgar
 */
public class LocalsValueResolver implements ValueResolver {

  @Override
  public Object resolve(final Object context, final String name) {
    Object value = null;
    if (context instanceof Locals) {
      value = ((Locals) context).get(name).orElse(null);
    }
    return value == null ? UNRESOLVED : value;
  }

  @Override
  public Object resolve(final Object context) {
    if (context instanceof Locals) {
      return context;
    }
    return UNRESOLVED;
  }

  @Override
  public Set<Entry<String, Object>> propertySet(final Object context) {
    if (context instanceof Locals) {
      return ((Locals) context).attributes().entrySet();
    }
    return Collections.emptySet();
  }

}
