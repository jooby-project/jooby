/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.graphql;

import graphql.GraphQL;
import io.jooby.Context;

import javax.annotation.Nonnull;

public class BlockingGraphQLHandler extends GraphQLHandler {

  public BlockingGraphQLHandler(GraphQL graphQL) {
    super(graphQL);
  }

  @Nonnull @Override public Object apply(@Nonnull Context ctx) {
    return graphQL.execute(newExecutionInput(ctx));
  }
}
