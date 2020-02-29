package io.jooby.maven;

import io.jooby.run.JoobyRunOptions;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunMojoTest {

  @Test
  public void ensureConfigurationOptions() {
    Stream.of(JoobyRunOptions.class.getDeclaredFields())
        .filter(field -> !field.getName().equals("projectName") && !Modifier.isStatic(field.getModifiers()))
        .forEach(field -> {
          try {
            Field target = FieldUtils.getField(RunMojo.class, field.getName(), true);
            assertEquals(field.getGenericType(), target.getGenericType(), field.toString());
          } catch (Exception x) {
            throw new IllegalStateException(x);
          }
        });
  }
}
