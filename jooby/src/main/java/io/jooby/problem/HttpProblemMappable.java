/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem;

import edu.umd.cs.findbugs.annotations.NonNull;

/**
 * Implementing {@link HttpProblemMappable} allows to control the transformation of exception into
 * {@link HttpProblem}. {@link ProblemDetailsHandler} rely on `toHttpProblem()` method when it is
 * available.
 *
 * @author kliushnichenko
 * @since 3.4.2
 */
public interface HttpProblemMappable {
  @NonNull HttpProblem toHttpProblem();
}
