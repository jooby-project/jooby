/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.langchain4j;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

class BuiltInModelTest {

  @Test
  @DisplayName("Should resolve enum ignoring case")
  void resolveEnum() {
    assertEquals(BuiltInModel.OPENAI, BuiltInModel.resolve("openai"));
    assertEquals(BuiltInModel.ANTHROPIC, BuiltInModel.resolve("Anthropic"));
    assertEquals(BuiltInModel.OLLAMA, BuiltInModel.resolve("OLLAMA"));
    assertEquals(BuiltInModel.JLAMA, BuiltInModel.resolve("jLama"));
  }

  @Test
  @DisplayName("Should throw exception for unknown provider")
  void resolveUnknown() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> {
              BuiltInModel.resolve("vertex");
            });
    assertTrue(exception.getMessage().contains("Unsupported LangChain4j provider"));
  }

  @Test
  @DisplayName("Should parse timeout and temp values using internal helpers")
  void testConfigurationHelpers() {
    Config config = ConfigFactory.parseMap(Map.of("timeout", "15s", "temperature", 0.5));

    // Access helper methods via an enum instance
    BuiltInModel tester = BuiltInModel.OPENAI;

    assertEquals(15, tester.getTimeout(config, null).getSeconds());
    assertEquals(0.5, tester.getTemp(config));
  }

  @Test
  @DisplayName("Missing dependency check should throw IllegalStateException")
  void missingDependencyCheck() {
    BuiltInModel tester = BuiltInModel.OPENAI;

    IllegalStateException exception =
        assertThrows(
            IllegalStateException.class,
            () -> {
              // Intentionally passing a garbage class name to force the ClassNotFoundException
              tester.check("dev.langchain4j.fake.MissingClass", "langchain4j-fake");
            });

    assertTrue(exception.getMessage().contains("Provider dependency missing"));
  }
}
