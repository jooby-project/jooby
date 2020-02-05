/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import io.jooby.exception.ProvisioningException;

import javax.annotation.Nonnull;
import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Usage exceptions. They provide a descriptive message with a link for a detailed section.
 *
 * @since 2.1.0
 */
public class Usage extends RuntimeException {

  /**
   * Creates a new Usage exception.
   *
   * @param message Message.
   * @param id Link to detailed section.
   */
  public Usage(@Nonnull String message, @Nonnull String id) {
    super((message + "\nFor more details, please visit: " + System
        .getProperty("jooby.host", "https://jooby.io") + "/usage#" + id));
  }

  /**
   * Creates a mvc route missing exception.
   *
   * @param mvcRoute Mvc route.
   * @return Usage exception.
   */
  public static @Nonnull Usage mvcRouterNotFound(@Nonnull Class mvcRoute) {
    return apt("Router not found: `" + mvcRoute.getName()
        + "`. Make sure Jooby annotation processor is configured properly.", "router-not-found");
  }

  /**
   * Thrown when the reflective bean converter has no access to a parameter name. Compilation
   * must be done using <code>parameters</code> compiler option.
   *
   * @param parameter Parameter.
   * @return Usage exception.
   */
  public static @Nonnull Usage parameterNameNotPresent(@Nonnull Parameter parameter) {
    Executable executable = parameter.getDeclaringExecutable();
    int p = Stream.of(executable.getParameters()).collect(Collectors.toList()).indexOf(parameter);
    String message = "Unable to provision parameter at position: '" + p + "', require by: "
        + ProvisioningException.toString(parameter.getDeclaringExecutable())
        + ". Parameter's name is missing";
    return new Usage(message, "bean-converter-parameter-name-missing");
  }

  private static Usage apt(String message, String id) {
    return new Usage(message, "annotation-processing-tool-" + id);
  }
}
