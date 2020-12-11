package io.jooby.apt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import io.jooby.SneakyThrows;

public class JaxrsTest {

  @Test
  public void shouldValidateJaxRSNames() {
    ClassLoader loader = getClass().getClassLoader();
    List<String> annotations = Stream.of(Annotations.class.getDeclaredFields())
        .filter(it -> it.getName().startsWith("JAXRS_"))
        .map(SneakyThrows.throwingFunction(it -> it.get(null).toString()))
        .collect(Collectors.toList());

    assertEquals(15, annotations.size());
    annotations.forEach(SneakyThrows.throwingConsumer(annotation -> loader.loadClass(annotation)));
  }
}
