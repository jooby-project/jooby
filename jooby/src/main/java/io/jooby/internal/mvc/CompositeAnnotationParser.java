/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

public class CompositeAnnotationParser implements MvcAnnotationParser {
  private MvcAnnotationParserBase[] parsers;

  public CompositeAnnotationParser(MvcAnnotationParserBase... parsers) {
    this.parsers = parsers;
  }

  @Override public List<MvcAnnotation> parse(Method method) {
    for (MvcAnnotationParserBase parser : parsers) {
      List<MvcAnnotation> models = parser.parse(method);
      if (models.size() > 0) {
        return models;
      }
    }
    return Collections.emptyList();
  }
}
