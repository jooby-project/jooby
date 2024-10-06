/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.exception;

import java.util.List;

import edu.umd.cs.findbugs.annotations.NonNull;
import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;

/**
 * Whether a HTTP method isn't supported. The {@link #getAllow()} contains the supported HTTP
 * methods.
 *
 * @since 2.4.1
 * @author edgar
 */
public class MethodNotAllowedException extends StatusCodeException {
  private final List<String> allow;

  /**
   * Creates a new method not allowed exception.
   *
   * @param method Requested method.
   * @param allow Allow methods.
   */
  public MethodNotAllowedException(@NonNull String method, @NonNull List<String> allow) {
    super(StatusCode.METHOD_NOT_ALLOWED, method);
    this.allow = allow;
  }

  /**
   * Requested method.
   *
   * @return Requested method.
   */
  public String getMethod() {
    return getMessage();
  }

  /**
   * Allow methods.
   *
   * @return Allow methods.
   */
  public List<String> getAllow() {
    return allow;
  }

  @Override
  public @NonNull HttpProblem toHttpProblem() {
    return HttpProblem.valueOf(statusCode,
        statusCode.reason(),
        "HTTP method '" + getMethod() + "' is not allowed. Allowed methods are: " + allow
    );
  }
}
