/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.instance;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import io.jooby.apt.ProcessorRunner;

public class DefaultControllerTest {

  @Test
  public void shouldGenerateDIDefaultConstructor() throws Exception {
    new ProcessorRunner(new DIController(new NoDIController()))
        .withSourceCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public DIController_() {
                        this(DIController.class);
                      }
                      """);
            });
  }

  @Test
  public void shouldGenerateDefaultConstructorWithoutDI() throws Exception {
    new ProcessorRunner(new NoDIController())
        .withSourceCode(
            source -> {
              assertThat(source)
                  .containsIgnoringWhitespaces(
                      """
                      public NoDIController_() {
                        this(io.jooby.SneakyThrows.singleton(NoDIController::new));
                      }
                      """);
            });
  }
}
