/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.apt;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.jooby.internal.apt.Annotations;

public class AnnotationTypesTest {
  @Test
  public void annotationTypes() throws IllegalAccessException, ClassNotFoundException {
    List<Field> fields = listFields();
    assertTrue(fields.size() > 0);

    ClassLoader classLoader = getClass().getClassLoader();
    for (Field field : fields) {
      String className = (String) field.get(null);
      assertNotNull(classLoader.loadClass(className));
    }
  }

  private List<Field> listFields() {
    return Stream.of(Annotations.class.getDeclaredFields())
        .filter(it -> it.getType() == String.class)
        .collect(Collectors.toList());
  }
}
