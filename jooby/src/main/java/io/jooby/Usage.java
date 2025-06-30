/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby;

import java.lang.reflect.Executable;
import java.lang.reflect.Parameter;
import java.util.stream.Stream;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.exception.ProvisioningException;

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
   * @param id Link to a detailed section.
   */
  public Usage(@NonNull String message, @NonNull String id) {
    this(
        (message
            + "\nFor more details, please visit: "
            + System.getProperty("jooby.host", "https://jooby.io")
            + "/usage#"
            + id));
  }

  protected Usage(@NonNull String message) {
    super(message);
  }

  public static @NonNull Usage noSession() {
    return new Usage("No session available. See https://jooby.io/#session-in-memory-session");
  }

  /**
   * Creates a mvc route missing exception.
   *
   * @param mvcRoute Mvc route.
   * @return Usage exception.
   */
  public static @NonNull Usage mvcRouterNotFound(@NonNull Class mvcRoute) {
    return apt(
        "Router not found: `"
            + mvcRoute.getName()
            + "`. Make sure Jooby annotation processor is configured properly.",
        "router-not-found");
  }

  /**
   * Thrown when the reflective bean converter has no access to a parameter name. Compilation must
   * be done using <code>parameters</code> compiler option.
   *
   * @param parameter Parameter.
   * @return Usage exception.
   */
  public static @NonNull Usage parameterNameNotPresent(@NonNull Parameter parameter) {
    Executable executable = parameter.getDeclaringExecutable();
    int p = Stream.of(executable.getParameters()).toList().indexOf(parameter);
    String message =
        "Unable to provision parameter at position: '"
            + p
            + "', require by: "
            + ProvisioningException.toString(parameter.getDeclaringExecutable())
            + ". Parameter's name is missing";
    return new Usage(message, "bean-converter-parameter-name-missing");
  }

  private static Usage apt(String message, String id) {
    return new Usage(message, "annotation-processing-tool-" + id);
  }
}
