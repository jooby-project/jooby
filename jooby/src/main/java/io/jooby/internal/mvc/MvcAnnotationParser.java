/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.mvc;

import java.lang.reflect.Method;
import java.util.List;

public interface MvcAnnotationParser {
  List<MvcAnnotation> parse(Method method);

  static MvcAnnotationParser create(ClassLoader loader) {
    try {
      loader.loadClass("javax.ws.rs.GET");
      return new CompositeAnnotationParser(new JoobyAnnotationParser(),
          new JaxrsAnnotationParser());
    } catch (ClassNotFoundException e) {
      return new JoobyAnnotationParser();
    }
  }

}
