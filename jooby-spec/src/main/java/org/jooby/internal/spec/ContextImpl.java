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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import org.jooby.spec.RouteSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.javaparser.JavaParser;
import com.github.javaparser.ParseException;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.Node;
import com.google.common.base.Throwables;

public class ContextImpl implements Context {

  /** The logging system. */
  private final Logger log = LoggerFactory.getLogger(getClass());

  private TypeResolver typeResolver;

  private SourceResolver sourceResolver;

  public ContextImpl(final TypeResolver typeResolver, final SourceResolver sourceResolver) {
    this.typeResolver = typeResolver;
    this.sourceResolver = sourceResolver;
  }

  @Override
  public ClassLoader classLoader() {
    return typeResolver.classLoader();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Optional<List<RouteSpec>> parseSpec(final Type type) {
    String fname = "/" + type.getTypeName().replace(".", "/") + ".spec";
    try (ObjectInputStream stream = new ObjectInputStream(
        getClass().getResourceAsStream(fname))) {
      List<RouteSpec> specs = (List<RouteSpec>) stream.readObject();
      return Optional.of(specs);
    } catch (IOException | ClassNotFoundException ex) {
      throw Throwables.propagate(ex);
    } catch (NullPointerException ex) {
      // stream not found
      return Optional.empty();
    }
  }

  @Override
  public Optional<CompilationUnit> parse(final Type type) {
    Optional<Reader> src = sourceResolver.resolveSource(type);
    if (src.isPresent()) {
      try {
        CompilationUnit unit = JavaParser.parse(src.get(), true);
        return Optional.of(unit);
      } catch (ParseException ex) {
        log.error("Unable to parse " + type, ex);
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<Type> resolveType(final Node node, final String name) {
    return typeResolver.resolveType(node, name);
  }

  @Override
  public Optional<Reader> resolveSource(final Type type) {
    return sourceResolver.resolveSource(type);
  }

}
