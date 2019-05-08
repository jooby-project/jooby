package io.jooby.maven;

import io.jooby.run.JoobyRunConf;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.stream.Stream;

import static junit.framework.Assert.assertEquals;

public class RunMojoTest {

  @Test
  public void ensureConfigurationOptions() {
    Stream.of(JoobyRunConf.class.getDeclaredFields())
        .filter(field -> !field.getName().equals("projectName"))
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
