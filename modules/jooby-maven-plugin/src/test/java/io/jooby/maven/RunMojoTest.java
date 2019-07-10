package io.jooby.maven;

import io.jooby.run.JoobyRunOptions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.stream.Stream;

import static junit.framework.Assert.assertEquals;

public class RunMojoTest {

  @Test
  public void ensureConfigurationOptions() {
    Stream.of(JoobyRunOptions.class.getDeclaredFields())
        .filter(field -> !field.getName().equals("projectName") && !Modifier.isStatic(field.getModifiers()))
        .forEach(field -> {
          try {
            Field target = RunMojo.class.getDeclaredField(field.getName());
            assertEquals(field.getGenericType(), target.getGenericType());
          } catch (Exception x) {
            throw new IllegalStateException(x);
          }
        });
  }
}
