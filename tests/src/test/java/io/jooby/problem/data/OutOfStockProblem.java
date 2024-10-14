/*
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.problem.data;

import java.net.URI;
import java.util.List;

import io.jooby.StatusCode;
import io.jooby.problem.HttpProblem;

public class OutOfStockProblem extends HttpProblem {

  private static final URI TYPE = URI.create("https://example.org/out-of-stock");

  public OutOfStockProblem(final String product) {
    super(
        builder()
            .type(TYPE)
            .title("Out of Stock")
            .status(StatusCode.BAD_REQUEST)
            .detail(String.format("'%s' is no longer available", product))
            .param("suggestions", List.of("Coffee Grinder MX-17", "Coffee Grinder MX-25")));
  }
}
