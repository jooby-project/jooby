package io.jooby.internal.openapi;

import java.util.List;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;

/**
 * Fix for:
 *
 * Caused by: java.lang.IllegalArgumentException: Conflicting setter definitions for property
 * 	at com.fasterxml.jackson.databind.introspect.POJOPropertyBuilder.getSetter(POJOPropertyBuilder.java:504)
 * 	at io.swagger.v3.core.jackson.ModelResolver.ignore(ModelResolver.java:995)
 * 	at io.swagger.v3.core.jackson.ModelResolver.resolve(ModelResolver.java:572)
 */
class ConflictiveSetter extends AnnotationIntrospector {
  @Override public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public AnnotatedMethod resolveSetterConflict(MapperConfig<?> config, AnnotatedMethod setter1,
      AnnotatedMethod setter2) {
    Class<?> cls1 = setter1.getRawParameterType(0);
    Class<?> cls2 = setter2.getRawParameterType(0);
    if (isArrayLike(cls1, cls2)) {
      return setter1;
    }
    if (isArrayLike(cls2, cls1)) {
      return setter2;
    }
    return null;
  }

  /**
   * Returns true if type1 is a list and type2 an array.
   *
   * @param type1 Type 1.
   * @param type2 Type 2.
   * @return True if type1 is a list and type2 an array.
   */
  private boolean isArrayLike(Class type1, Class type2) {
    return List.class.isAssignableFrom(type1) && type2.isArray();
  }
}
