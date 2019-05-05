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
