/**
 * Jooby https://jooby.io
 * Apache License Version 2.0 https://jooby.io/LICENSE.txt
 * Copyright 2014 Edgar Espina
 */
package io.jooby.internal.graphql;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import io.jooby.Context;
import io.jooby.Route;
import io.jooby.Router;

import edu.umd.cs.findbugs.annotations.NonNull;
import java.util.Collections;
import java.util.Map;

public class GraphQLHandler implements Route.Handler {
  private static final Gson json = new GsonBuilder()
      .create();

  protected GraphQL graphQL;

  public GraphQLHandler(GraphQL graphQL) {
    this.graphQL = graphQL;
  }

  @NonNull @Override public Object apply(@NonNull Context ctx) {
    return graphQL.executeAsync(newExecutionInput(ctx))
        .thenApply(ExecutionResult::toSpecification);
  }

  protected final ExecutionInput newExecutionInput(@NonNull Context ctx) {
    GraphQLRequest request;
    if (ctx.getMethod().equals(Router.POST)) {
      request = ctx.body(GraphQLRequest.class);
    } else {
      request = new GraphQLRequest();
      String query = ctx.query("query").value();
      String operationName = ctx.query("operationName").valueOrNull();
      Map<String, Object> variables = ctx.query("variables").toOptional()
          .filter(string -> !string.equals("{}"))
          .map(str -> json.fromJson(str,Map.class))
          .orElseGet(Collections::emptyMap);
      request.setOperationName(operationName);
      request.setQuery(query);
      request.setVariables(variables);
    }
    return ExecutionInput.newExecutionInput(request.getQuery())
        .operationName(request.getOperationName())
        .context(ctx)
        .variables(request.getVariables())
        .build();
  }
}
