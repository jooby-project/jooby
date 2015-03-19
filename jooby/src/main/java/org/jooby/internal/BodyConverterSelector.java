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
package org.jooby.internal;

import static java.util.Objects.requireNonNull;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.jooby.BodyFormatter;
import org.jooby.MediaType;
import org.jooby.BodyParser;
import org.jooby.View;
import org.jooby.util.Collectors;

import com.google.inject.TypeLiteral;

/**
 * Choose or select a {@link BodyParser} or {@link BodyFormatter} using {@link MediaType media
 * types.}. Examples:
 *
 * <pre>
 *   // selector with html and json converters
 *   selector = new BodyConverterSelector(Sets.newLinkedHashSet(html, json));
 *
 *   // asking for html, produces the html converter
 *   assertEquals(html, selector.get(MediaType.html));
 *
 *   // asking for json, produces the json converter
 *   assertEquals(json, selector.get(MediaType.json));
 *
 *   // asking for * / *, produces the first matching converter
 *   assertEquals(html, selector.get(MediaType.all));
 * </pre>
 *
 * @author edgar
 * @since 0.1.0
 */
@Singleton
public class BodyConverterSelector {

  /**
   * The available converters in the system.
   */
  private final Set<BodyFormatter> formatters;

  private final Set<BodyParser> parsers;

  private List<MediaType> viewableTypes;

  /**
   * Creates a new {@link BodyConverterSelector}.
   *
   * @param parsers The available body parsers in the system.
   * @param formatters The available body formatters in the system.
   */
  @Inject
  public BodyConverterSelector(final Set<BodyParser> parsers,
      final Set<BodyFormatter> formatters) {
    this.parsers = requireNonNull(parsers, "The parsers is required.");
    this.formatters = requireNonNull(formatters, "The formatters is required.");
    this.viewableTypes = this.formatters.stream().filter(View.Engine.class::isInstance)
        .flatMap(engine -> engine.types().stream())
        .collect(Collectors.toList());
  }

  public List<MediaType> viewableTypes() {
    return viewableTypes;
  }

  public Optional<BodyParser> parser(final TypeLiteral<?> type,
      final Iterable<MediaType> types) {
    requireNonNull(type, "Type literal is required.");
    requireNonNull(types, "Types are required.");

    for (BodyParser parser : parsers) {
      if (parser.canParse(type)) {
        for (MediaType mtype : types) {
          Optional<MediaType> found = parser.types()
              .stream()
              .filter(it -> mtype.matches(it))
              .findFirst();
          if (found.isPresent()) {
            return Optional.of(parser);
          }
        }
      }
    }

    return Optional.empty();
  }

  public Optional<BodyFormatter> formatter(final Object message,
      final Iterable<MediaType> types) {
    requireNonNull(message, "A message is required.");
    requireNonNull(types, "Types are required.");

    Class<?> clazz = message.getClass();

    Predicate<BodyFormatter> noop = (f) -> true;

    Predicate<BodyFormatter> viewable = (f) -> {
      if (f instanceof View.Engine) {
        String engine = ((View) message).engine();
        return engine.isEmpty() || ((View.Engine) f).name().equals(engine);
      }
      return true;
    };

    Predicate<BodyFormatter> nameMatcher = message instanceof View ? viewable : noop;

    for (BodyFormatter formatter : formatters) {
      if (formatter.canFormat(clazz) && nameMatcher.test(formatter)) {
        for (MediaType type : types) {
          Optional<MediaType> found = formatter.types()
              .stream()
              .filter(it -> type.matches(it))
              .findFirst();
          if (found.isPresent()) {
            return Optional.of(formatter);
          }
        }
      }
    }

    return Optional.empty();
  }

}
