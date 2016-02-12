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
package org.jooby.internal.spec;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.github.javaparser.ast.Node;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

public class TypeResolverImpl implements TypeResolver {

  private ClassLoader loader;

  public TypeResolverImpl(final ClassLoader loader) {
    this.loader = loader;
  }

  @Override
  public ClassLoader classLoader() {
    return loader;
  }

  @Override
  public Optional<Type> resolveType(final Node n, final String name) {
    Set<String> dependencies = new DependencyCollector().accept(n);
    return dependencies.stream()
        .filter(it -> it.endsWith("." + name))
        .map(this::type)
        .findFirst()
        .orElseGet(() -> guessType(dependencies, name));
  }

  private Optional<Type> guessType(final Set<String> dependencies, final String name) {
    Function<String, List<String>> names = p -> {
      ImmutableList.Builder<String> builder = ImmutableList.builder();
      builder.add(p + "." + name);
      int $ = name.indexOf('$');
      if ($ > 0) {
        String dclass = name.substring(0, $);
        if (p.endsWith("." + dclass)) {
          builder.add(p + "$" + name.substring($ + 1));
        }
      }
      return builder.build();
    };
    for (String pkg : dependencies) {
      for (String qname : names.apply(pkg)) {
        Optional<Type> type = type(qname);
        if (type.isPresent()) {
          return type;
        }
      }
    }
    return type(name);
  }

  private Optional<Type> type(final String name) {
    return expand(name).stream()
        .map(n -> {
          try {
            Type type = loader.loadClass(n);
            return type;
          } catch (ClassNotFoundException ex) {
            return null;
          }
        }).filter(c -> c != null)
        .findFirst();
  }

  private Set<String> expand(final String name) {
    String[] segments = name.split("\\.");
    StringBuilder buff = new StringBuilder();
    for (String segment : segments) {
      buff.append(segment);
      char sep = Character.isUpperCase(segment.charAt(0)) ? '$' : '.';
      buff.append(sep);
    }
    buff.setLength(buff.length() - 1);
    return ImmutableSet.of(name, buff.toString());
  }

}
