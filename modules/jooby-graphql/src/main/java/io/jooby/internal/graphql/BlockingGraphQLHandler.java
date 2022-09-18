/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.graphql;

import graphql.GraphQL;
import io.jooby.Context;

import edu.umd.cs.findbugs.annotations.NonNull;

public class BlockingGraphQLHandler extends GraphQLHandler {

  public BlockingGraphQLHandler(GraphQL graphQL) {
    super(graphQL);
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) {
    return graphQL.execute(newExecutionInput(ctx));
  }
}
