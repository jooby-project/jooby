/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package javadoc;

import static io.jooby.internal.openapi.javadoc.MiniYamlDocParser.parse;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class ExtensionJavaDocParserTest {

  @Test
  public void shouldParseMapLike() {
    assertEquals(
        Map.of("x-badges", Map.of("icon", Map.of("name", "Beta", "color", "Blue"))),
        parse(List.of("x-badges.icon.name", "Beta", "x-badges.icon.color", "Blue")));
    assertEquals(
        Map.of("x-badges", Map.of("name", "Beta", "color", "Blue")),
        parse(List.of("x-badges.name", "Beta", "x-badges.color", "Blue")));
  }

  @Test
  public void shouldParseListOfMap() {
    assertEquals(
        Map.of(
            "x-badges",
            Map.of(
                "icon",
                List.of(
                    Map.of("name", "Beta", "color", "Blue"),
                    Map.of("name", "Final", "color", "Red")))),
        parse(
            List.of(
                "x-badges.icon.name",
                "Beta",
                "x-badges.icon.color",
                "Blue",
                "x-badges.icon.name",
                "Final",
                "x-badges.icon.color",
                "Red")));
    assertEquals(
        Map.of(
            "x-badges",
            List.of(
                Map.of("name", "Beta", "color", "Blue"), Map.of("name", "Final", "color", "Red"))),
        parse(
            List.of(
                "x-badges.name",
                "Beta",
                "x-badges.color",
                "Blue",
                "x-badges.name",
                "Final",
                "x-badges.color",
                "Red")));
  }

  @Test
  public void shouldForceArrayOnSingleElements() {
    // properties starting with `-` must be always array
    assertEquals(
        Map.of("x-badges", Map.of("icon", List.of(Map.of("name", "Beta", "color", "Blue")))),
        parse(List.of("x-badges.icon.-name", "Beta", "x-badges.icon.color", "Blue")));
    assertEquals(
        Map.of("x-badges", List.of(Map.of("name", "Beta", "color", "Blue"))),
        parse(List.of("x-badges.-name", "Beta", "x-badges.color", "Blue")));
  }
}
