/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package tests.verifyarg;

import io.jooby.apt.ProcessorRunner;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VerifyArgTypeTest {

  @ParameterizedTest
  @MethodSource("provideControllers")
  public void compileController_illegalArgumentType_shouldThrowException(Object controller) {
    RuntimeException ex = assertThrows(RuntimeException.class,
        () -> new ProcessorRunner(controller));

    assertTrue(ex.getMessage().contains("Illegal argument type at"));
  }

  private static List<Arguments> provideControllers() {
    return List.of(
        Arguments.of(new ControllerFlash()),
        Arguments.of(new ControllerFlashOpt()),
        Arguments.of(new ControllerCookie()),
        Arguments.of(new ControllerSession()),
        Arguments.of(new ControllerHeader())
    );
  }
}
